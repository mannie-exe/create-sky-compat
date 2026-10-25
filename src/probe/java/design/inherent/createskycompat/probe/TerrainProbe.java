package design.inherent.createskycompat.probe;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import design.inherent.createskycompat.terrain.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.RandomState;
import org.sathrek.sky_archipelago.worldgen.generator.core.SkyIslandChunkGenerator;
import org.sathrek.sky_archipelago.config.SkyIslandGeneratorSettings;
import org.sathrek.sky_archipelago.config.SkyIslandSettings;
import org.sathrek.sky_archipelago.worldgen.generator.field.IslandField;
import org.sathrek.sky_archipelago.worldgen.generator.field.IslandField.IslandFamily;
import java.nio.file.*;
import java.util.*;

/** Bounded native density survey. No chunk generation or neighboring-chunk loads. */
final class TerrainProbe {
    private TerrainProbe() {}

    static void checkEmbeddedSettings(SkyIslandChunkGenerator generator) {
        var actual = SkyIslandGeneratorSettings.CODEC.encodeStart(JsonOps.INSTANCE,
                SkyIslandGeneratorSettings.fromRuntimeSettings(generator.resolvedSkyIslandSettings())).getOrThrow();
        var expected = SkyIslandGeneratorSettings.CODEC.encodeStart(JsonOps.INSTANCE,
                SkyIslandGeneratorSettings.fromRuntimeSettings(generator.embeddedSettings().orElseThrow().toRuntimeSettings())).getOrThrow();
        if (!actual.equals(expected)) throw new AssertionError("Dedicated server ignored embedded preset settings");
    }

    static void run(MinecraftServer server, SkyIslandSettings baseline, Path output) throws Exception {
        var report = new JsonObject();
        report.addProperty("evidence", "native-density-samples; actual preset biome filtering; before carving/features");
        report.add("profile", new Gson().toJsonTree(GenerationProfile.INITIAL));
        var rows = new JsonArray(); long started = System.nanoTime();
        for (long seed : new long[]{1, 42, 8675309, 424242}) {
            for (boolean adapted : new boolean[]{false, true}) {
                var preset = server.registryAccess().registryOrThrow(Registries.WORLD_PRESET)
                        .get(ResourceLocation.parse(adapted ? GenerationProfile.ID : "sky_archipelago:sky_islands"));
                var generator = (SkyIslandChunkGenerator)preset.createWorldDimensions().overworld();
                var selectedSettings = generator.resolvedSkyIslandSettings();
                if (adapted) checkEmbeddedSettings(generator);
                var random = RandomState.create(generator.generatorSettings().value(),
                        server.registryAccess().registryOrThrow(Registries.NOISE).asLookup(), seed);
                var field = ((GeologicalGenerator)(Object)generator).createSky$field(random);
                var previews = field.collectIslandPreviewsInRadius(0, 0, 16000, selectedSettings);
                var anchors = previews.stream().filter(p -> p.family() == IslandFamily.ANCHOR_PLATEAU)
                        .sorted(Comparator.comparingDouble(p -> Math.hypot(p.x(), p.z()))).toList();
                var row = new JsonObject(); row.addProperty("seed", seed); row.addProperty("adapted", adapted);
                row.add("settings", SkyIslandGeneratorSettings.CODEC.encodeStart(JsonOps.INSTANCE,
                        SkyIslandGeneratorSettings.fromRuntimeSettings(selectedSettings)).getOrThrow());
                row.add("descriptors", new Gson().toJsonTree(previews));
                var distances = new JsonArray();
                for (var anchor : anchors) {
                    if (Math.hypot(anchor.x(), anchor.z()) > 10000) continue;
                    double nearest = anchors.stream().filter(p -> p != anchor)
                            .mapToDouble(p -> Math.hypot(p.x() - anchor.x(), p.z() - anchor.z())).min().orElseThrow();
                    distances.add(nearest);
                }
                row.add("nearestAnchorDistances", distances);
                var islands = new JsonArray();
                for (var anchor : anchors.stream().limit(Integer.getInteger("neighbors.probe.samples", 4)).toList()) {
                    int step = Math.max(8, anchor.radius() / 4);
                    var island = new JsonObject(); island.add("descriptor", new Gson().toJsonTree(anchor));
                    var columns = new JsonArray(); long estimatedVolume = 0;
                    int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE, occupied = 0;
                    for (int dx = -5; dx <= 5; dx++) for (int dz = -5; dz <= 5; dz++) {
                        int x = anchor.x() + dx * step, z = anchor.z() + dz * step;
                        var descriptor = field.sampleDominantIslandDescriptor(x, z, selectedSettings);
                        if (descriptor == null || descriptor.family() != IslandFamily.ANCHOR_PLATEAU
                                || descriptor.centerX() != anchor.x() || descriptor.centerZ() != anchor.z()) continue;
                        var segments = field.sampleSolidSegments(x, z, selectedSettings);
                        int thickness = segments.stream().mapToInt(s -> s.thickness()).sum();
                        if (thickness == 0) continue;
                        occupied++;
                        for (var segment : segments) {
                            minY = Math.min(minY, segment.bottomY()); maxY = Math.max(maxY, segment.topY());
                        }
                        var column = new JsonObject(); column.addProperty("x", x); column.addProperty("z", z);
                        column.addProperty("solid", thickness); column.addProperty("interior", dx * dx + dz * dz <= 4);
                        column.add("segments", new Gson().toJsonTree(segments)); columns.add(column);
                        estimatedVolume += (long)thickness * step * step;
                    }
                    island.addProperty("step", step); island.addProperty("occupiedSamples", occupied);
                    island.addProperty("sampledMinY", occupied == 0 ? 0 : minY);
                    island.addProperty("sampledMaxY", occupied == 0 ? 0 : maxY);
                    island.addProperty("estimatedSolidVolume", estimatedVolume); island.add("columns", columns);
                    islands.add(island);
                    System.out.println("CREATE_SKY_TERRAIN " + seed + " adapted=" + adapted + " anchor="
                            + anchor.x() + "," + anchor.z() + " samples=" + occupied + " y=" + minY + ".." + maxY);
                }
                row.add("islands", islands); rows.add(row);
            }
        }
        report.addProperty("elapsedSeconds", (System.nanoTime() - started) / 1e9);
        report.add("runs", rows);
        Files.writeString(output, new GsonBuilder().setPrettyPrinting().create().toJson(report));
    }
}
