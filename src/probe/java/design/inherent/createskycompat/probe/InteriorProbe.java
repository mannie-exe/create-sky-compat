package design.inherent.createskycompat.probe;

import com.google.gson.*;
import design.inherent.createskycompat.terrain.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.RandomState;
import org.sathrek.sky_archipelago.worldgen.generator.core.SkyIslandChunkGenerator;
import org.sathrek.sky_archipelago.worldgen.generator.field.IslandField;
import org.sathrek.sky_archipelago.worldgen.generator.field.TerrainColumn;
import java.nio.file.*;
import java.util.*;

/** Native interior sections plus independent direct/snapshot query ordering. */
final class InteriorProbe {
    private InteriorProbe() {}

    static void run(MinecraftServer server, Path output) throws Exception {
        var level = server.overworld();
        var generator = (SkyIslandChunkGenerator)level.getChunkSource().getGenerator();
        var settings = generator.resolvedSkyIslandSettings();
        var field = ((GeologicalGenerator)(Object)generator).createSky$field(level.getChunkSource().randomState());
        var otherRandom = RandomState.create(generator.generatorSettings().value(),
                server.registryAccess().registryOrThrow(Registries.NOISE).asLookup(), level.getSeed());
        var otherGenerator = new SkyIslandChunkGenerator(generator.getBiomeSource(), generator.generatorSettings(), generator.embeddedSettings());
        var other = ((GeologicalGenerator)(Object)otherGenerator).createSky$field(otherRandom);
        if (field == other) throw new AssertionError("Order check requires independent fields");
        var candidates = new ArrayList<IslandField.IslandDescriptor>();
        for (int cx = -5; cx <= 5; cx++) for (int cz = -5; cz <= 5; cz++) {
            var members = ((ClusterField)(Object)field).createSky$members(cx, cz, settings);
            if (!members.isEmpty()) candidates.add(members.getFirst());
        }
        candidates.sort(Comparator.comparingDouble(p -> Math.hypot(p.centerX(), p.centerZ())));
        var selected = new LinkedHashMap<IslandInteriors.Kind, IslandField.IslandDescriptor>();
        for (var island : candidates) selected.putIfAbsent(IslandInteriors.kind(island, InteriorConfiguration.profile()), island);
        var rows = new JsonArray();
        long started = System.nanoTime(); long agreementChecks = 0;
        for (var entry : selected.entrySet()) {
            var island = entry.getValue();
            long exteriorVolume = 0, interiorVolume = 0, solidVolume = 0, waterVolume = 0, caveColumns = 0;
            int largestInteriorGap = 0;
            var sections = new JsonArray();
            int reach = island.maxRadius();
            for (int x = island.centerX() - reach; x <= island.centerX() + reach; x += 16)
                for (int z = island.centerZ() - reach; z <= island.centerZ() + reach; z += 16) {
                    var exterior = ((InteriorField)(Object)field).createSky$exteriorAt(x, z, settings);
                    var planning = (InteriorField)(Object)field;
                    var interior = planning.createSky$planningColumn(x, z, settings, TerrainStage.INTERIOR).segments();
                    var solid = field.sampleSolidSegments(x, z, settings);
                    long before = exterior.stream().mapToInt(TerrainColumn::thickness).sum();
                    long after = solid.stream().mapToInt(TerrainColumn::thickness).sum();
                    if (after > before) throw new AssertionError("Cavity added solid");
                    for (var segment : solid) for (int y = segment.bottomY(); y <= segment.topY(); y++) {
                        final int sy = y;
                        if (exterior.stream().noneMatch(s -> s.contains(sy))) throw new AssertionError("Solid outside exterior");
                    }
                    if (after < before) caveColumns++;
                    exteriorVolume += before; solidVolume += after;
                    interiorVolume += interior.stream().mapToInt(TerrainColumn::thickness).sum();
                    waterVolume += planning.createSky$waterAt(x, z, settings).sources().stream().mapToInt(TerrainColumn::thickness).sum();
                    // Sample the independent field through snapshot first, then direct column.
                    if ((x - island.centerX() + reach) % 128 == 0 && (z - island.centerZ() + reach) % 128 == 0) {
                        if (!planning.createSky$planningColumn(x, z, settings, TerrainStage.EXTERIOR).segments().equals(exterior)
                                || !((InteriorField)(Object)other).createSky$planningColumn(x, z, settings, TerrainStage.INTERIOR).segments().equals(interior))
                            throw new AssertionError("Planning stage disagrees with resolved column");
                        var snapshot = other.sampleChunkTerrainSnapshot(new ChunkPos(x >> 4, z >> 4), settings);
                        if (!solid.equals(snapshot.segmentsAt(x, z)) || !solid.equals(other.sampleSolidSegments(x, z, settings)))
                            throw new AssertionError("Snapshot/direct order changed solid intervals");
                        agreementChecks++;
                    }
                    for (var outer : exterior) {
                        int gap = 0;
                        for (int y = outer.bottomY(); y <= outer.topY(); y++) {
                            final int sy = y;
                            if (solid.stream().anyMatch(s -> s.contains(sy))) gap = 0;
                            else largestInteriorGap = Math.max(largestInteriorGap, ++gap);
                        }
                    }
                }
            for (int x = island.centerX() - reach; x <= island.centerX() + reach; x += 2) {
                var section = new JsonObject(); section.addProperty("x", x); section.addProperty("z", island.centerZ());
                section.add("exterior", new Gson().toJsonTree(((InteriorField)(Object)field).createSky$exteriorAt(x, island.centerZ(), settings)));
                section.add("interior", new Gson().toJsonTree(((InteriorField)(Object)field)
                        .createSky$planningColumn(x, island.centerZ(), settings, TerrainStage.INTERIOR).segments()));
                section.add("solid", new Gson().toJsonTree(field.sampleSolidSegments(x, island.centerZ(), settings)));
                section.add("water", new Gson().toJsonTree(((InteriorField)(Object)field).createSky$waterAt(x, island.centerZ(), settings).sources()));
                sections.add(section);
            }
            var row = new JsonObject(); row.addProperty("kind", entry.getKey().name());
            row.add("island", new Gson().toJsonTree(island)); row.addProperty("sampleStep", 16);
            row.addProperty("exteriorSampledBlocks", exteriorVolume); row.addProperty("solidSampledBlocks", solidVolume);
            row.addProperty("interiorSampledBlocks", interiorVolume); row.addProperty("waterSampledBlocks", waterVolume);
            row.addProperty("cavityColumns", caveColumns); row.addProperty("largestInteriorGap", largestInteriorGap);
            row.add("section", sections); rows.add(row);
        }
        var report = new JsonObject(); report.addProperty("seed", Long.toString(level.getSeed()));
        report.addProperty("evidence", "native exterior/solid intervals before classic carving, features and fluid ticks");
        report.add("interiors", new Gson().toJsonTree(InteriorConfiguration.profile()));
        report.addProperty("queryOrderChecks", agreementChecks); report.add("samples", rows);
        report.addProperty("elapsedSeconds", (System.nanoTime() - started) / 1e9); report.addProperty("complete", true);
        Files.writeString(output, new GsonBuilder().setPrettyPrinting().create().toJson(report) + "\n");
    }
}
