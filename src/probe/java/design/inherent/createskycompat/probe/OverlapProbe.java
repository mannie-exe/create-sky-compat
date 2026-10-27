package design.inherent.createskycompat.probe;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import design.inherent.createskycompat.terrain.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.levelgen.RandomState;
import org.sathrek.sky_archipelago.config.SkyIslandGeneratorSettings;
import org.sathrek.sky_archipelago.config.SkyIslandSettings;
import org.sathrek.sky_archipelago.worldgen.generator.core.SkyIslandChunkGenerator;
import org.sathrek.sky_archipelago.worldgen.generator.field.IslandField;
import java.nio.file.*;
import java.util.Comparator;

/** Matched native exterior columns; this does not claim feature or fluid verification. */
final class OverlapProbe {
    private OverlapProbe() {}

    static SkyIslandSettings mode(SkyIslandSettings settings, String mode) {
        var json = SkyIslandGeneratorSettings.CODEC.encodeStart(JsonOps.INSTANCE,
                SkyIslandGeneratorSettings.fromRuntimeSettings(settings)).getOrThrow().getAsJsonObject();
        json.getAsJsonObject("advanced").addProperty("terrain_overlap_mode", mode);
        return SkyIslandGeneratorSettings.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow().toRuntimeSettings();
    }

    static void run(MinecraftServer server, Path output) throws Exception {
        long began = System.nanoTime();
        var report = new JsonObject();
        report.addProperty("evidence", "matched native exterior columns before carvers, features and new interiors");
        report.add("profile", new Gson().toJsonTree(GenerationConfiguration.profile()));
        var rows = new JsonArray();
        for (long seed : new long[]{1, 42, 8675309}) {
            var preset = server.registryAccess().registryOrThrow(Registries.WORLD_PRESET)
                    .get(ResourceLocation.parse(GenerationProfile.ID));
            var generator = (SkyIslandChunkGenerator)preset.createWorldDimensions().overworld();
            var random = RandomState.create(generator.generatorSettings().value(),
                    server.registryAccess().registryOrThrow(Registries.NOISE).asLookup(), seed);
            var field = ((GeologicalGenerator)(Object)generator).createSky$field(random);
            var settings = generator.resolvedSkyIslandSettings();
            var voidSettings = mode(settings, "void");
            var overlapSettings = mode(settings, "overlap");
            var anchors = field.collectIslandPreviewsInRadius(0, 0, 12000, settings).stream()
                    .filter(p -> p.family() == IslandField.IslandFamily.ANCHOR_PLATEAU)
                    .sorted(Comparator.comparingDouble(p -> Math.hypot(p.x(), p.z()))).limit(2).toList();
            for (var anchor : anchors) {
                long columns = 0, occupied = 0, stacked = 0, voidVolume = 0, overlapVolume = 0;
                int maximumSegments = 0, maximumAdded = 0;
                var examples = new JsonArray();
                int reach = GenerationConfiguration.profile().clusterReach();
                for (int x = anchor.x() - reach; x <= anchor.x() + reach; x += 24)
                    for (int z = anchor.z() - reach; z <= anchor.z() + reach; z += 24) {
                        columns++;
                        var before = ((InteriorField)(Object)field).createSky$exteriorAt(x, z, voidSettings);
                        var after = ((InteriorField)(Object)field).createSky$exteriorAt(x, z, overlapSettings);
                        if (before.isEmpty() != after.isEmpty()
                                || !before.isEmpty() && !before.getFirst().equals(after.getFirst())) {
                            throw new AssertionError("Overlap changed the upper exterior at " + x + "," + z);
                        }
                        if (after.isEmpty()) continue;
                        occupied++;
                        long oldThickness = before.stream().mapToInt(s -> s.thickness()).sum();
                        long newThickness = after.stream().mapToInt(s -> s.thickness()).sum();
                        voidVolume += oldThickness; overlapVolume += newThickness;
                        maximumSegments = Math.max(maximumSegments, after.size());
                        if (after.size() > 1) stacked++;
                        int added = (int)(newThickness - oldThickness);
                        if (added > maximumAdded) {
                            maximumAdded = added;
                            var example = new JsonObject();
                            example.addProperty("x", x); example.addProperty("z", z);
                            example.add("segments", new Gson().toJsonTree(after));
                            examples.add(example);
                        }
                    }
                var row = new JsonObject();
                row.addProperty("seed", Long.toString(seed)); row.add("anchor", new Gson().toJsonTree(anchor));
                row.addProperty("sampleStep", 24); row.addProperty("columns", columns);
                row.addProperty("occupiedColumns", occupied); row.addProperty("stackedColumns", stacked);
                row.addProperty("voidSampledBlocks", voidVolume); row.addProperty("overlapSampledBlocks", overlapVolume);
                row.addProperty("maximumSegments", maximumSegments); row.add("lowerBodyExamples", examples);
                rows.add(row);
            }
        }
        report.add("samples", rows); report.addProperty("upperIntervalsUnchanged", true);
        report.addProperty("elapsedSeconds", (System.nanoTime() - began) / 1e9);
        report.addProperty("complete", true);
        Files.writeString(output, new GsonBuilder().setPrettyPrinting().create().toJson(report) + "\n");
    }
}
