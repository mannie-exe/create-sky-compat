package design.inherent.createskycompat.probe;

import com.google.gson.*;
import design.inherent.createskycompat.climate.IslandClimate;
import design.inherent.createskycompat.terrain.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import org.sathrek.sky_archipelago.worldgen.generator.core.SkyIslandChunkGenerator;
import org.sathrek.sky_archipelago.worldgen.generator.field.IslandField;

/** Contiguous native surfaces; raw quart biomes, smoothed lookups and weather have separate provenance. */
final class SurfaceClimateProbe {
    private SurfaceClimateProbe() {}

    static void run(MinecraftServer server, Path output) throws Exception {
        var level = server.overworld();
        var generator = (SkyIslandChunkGenerator)level.getChunkSource().getGenerator();
        var random = level.getChunkSource().randomState();
        var settings = generator.resolvedSkyIslandSettings();
        var field = ((GeologicalGenerator)(Object)generator).createSky$field(random);
        var sites = new LinkedHashMap<String, BlockPos>();
        var anchors = field.collectIslandPreviewsInRadius(0, 0, 20000, settings).stream()
                .filter(p -> p.family() == IslandField.IslandFamily.ANCHOR_PLATEAU)
                .sorted(Comparator.comparingDouble(p -> Math.hypot(p.x(), p.z()))).toList();
        outer: for (var anchor : anchors) for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            int x = anchor.x() + dx * anchor.radius() / 3, z = anchor.z() + dz * anchor.radius() / 3;
            var column = field.sampleColumn(x, z, settings);
            if (!column.exists()) continue;
            var p = new BlockPos(x, column.topY(), z);
            var biome = generator.getBiomeSource().getNoiseBiome(x >> 2, p.getY() >> 2, z >> 2, random.sampler());
            boolean cold = biome.value().coldEnoughToSnow(IslandClimate.temperaturePosition(p));
            if (cold || !biome.is(net.minecraft.tags.BiomeTags.IS_OCEAN)) sites.putIfAbsent(cold ? "cold" : "temperate", p);
            for (int offset : new int[]{-24, 24}) {
                var other = field.sampleColumn(x + offset, z, settings);
                if (!other.exists()) continue;
                var neighbor = generator.getBiomeSource().getNoiseBiome((x + offset) >> 2, other.topY() >> 2, z >> 2, random.sampler());
                if (neighbor.value().coldEnoughToSnow(new BlockPos(x + offset, 64, z)) != cold) sites.putIfAbsent("transition", p.offset(offset / 2, 0, 0));
            }
            if (sites.size() == 3) break outer;
        }
        var report = new JsonObject(); var areas = new JsonArray();
        report.addProperty("seed", level.getSeed());
        report.addProperty("evidence", "48x48 generated areas; one deliberate native precipitation sweep, not elapsed play time");
        var weather = ServerLevel.class.getDeclaredMethod("tickPrecipitation", BlockPos.class);
        weather.setAccessible(true);
        long started = System.nanoTime();
        for (var site : sites.entrySet()) {
            int minX = Math.floorDiv(site.getValue().getX(), 16) * 16 - 16;
            int minZ = Math.floorDiv(site.getValue().getZ(), 16) * 16 - 16;
            // An extra chunk border keeps native precipitation's area-loaded guard meaningful.
            for (int cx = (minX >> 4) - 1; cx <= (minX >> 4) + 3; cx++)
                for (int cz = (minZ >> 4) - 1; cz <= (minZ >> 4) + 3; cz++) level.getChunk(cx, cz);
            var area = new JsonObject(); area.addProperty("kind", site.getKey());
            area.addProperty("minX", minX); area.addProperty("minZ", minZ); area.addProperty("width", 48);
            area.add("beforeWeather", inspect(level, field, generator, minX, minZ));
            int cleared = 0;
            // Remove only generated layers, so the sweep must actually place snow instead of observing full layers.
            for (int x = minX; x < minX + 48; x++) for (int z = minZ; z < minZ + 48; z++) {
                int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                int bottom = field.sampleColumn(x, z, settings).topY() - 8;
                for (int y = bottom; y <= top; y++) {
                    var pos = new BlockPos(x, y, z);
                    if (level.getBlockState(pos).is(Blocks.SNOW)) { level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState()); cleared++; }
                }
            }
            area.addProperty("clearedSnowLayersBeforeSweep", cleared);
            level.setWeatherParameters(0, 6000, true, false); level.setRainLevel(1);
            for (int x = minX; x < minX + 48; x++) for (int z = minZ; z < minZ + 48; z++) {
                weather.invoke(level, new BlockPos(x, 0, z));
            }
            area.add("afterWeather", inspect(level, field, generator, minX, minZ));
            areas.add(area);
        }
        report.add("areas", areas);
        report.addProperty("allCategoriesFound", sites.size() == 3);
        report.addProperty("elapsedSeconds", (System.nanoTime() - started) / 1e9);
        report.addProperty("complete", sites.containsKey("temperate") && sites.containsKey("cold"));
        Files.writeString(output, new GsonBuilder().setPrettyPrinting().create().toJson(report));
    }

    private static JsonObject inspect(ServerLevel level, IslandField field, SkyIslandChunkGenerator generator, int minX, int minZ) {
        var result = new JsonObject(); var columns = new JsonArray();
        var counts = new TreeMap<String, Integer>(); var tags = new TreeMap<String, List<String>>();
        var settings = generator.resolvedSkyIslandSettings(); var random = level.getChunkSource().randomState();
        for (int x = minX; x < minX + 48; x++) for (int z = minZ; z < minZ + 48; z++) {
            var predicted = field.sampleColumn(x, z, settings);
            if (!predicted.exists()) continue;
            var chunk = level.getChunk(x >> 4, z >> 4);
            int y = predicted.topY(); var p = new BlockPos(x, y, z);
            var stored = chunk.getNoiseBiome(x >> 2, y >> 2, z >> 2);
            var visible = level.getBiome(p);
            var raw = generator.getBiomeSource().getNoiseBiome(x >> 2, y >> 2, z >> 2, random.sampler());
            var top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, p);
            int snowBlocks = 0, snowLayers = 0;
            for (int sy = y - 8; sy <= Math.min(level.getMaxBuildHeight() - 1, top.getY() + 1); sy++) {
                var state = chunk.getBlockState(new BlockPos(x, sy, z));
                if (state.is(Blocks.SNOW_BLOCK)) snowBlocks++;
                if (state.is(Blocks.SNOW)) snowLayers++;
            }
            var row = new JsonObject(); row.addProperty("x", x); row.addProperty("z", z); row.addProperty("surfaceY", y);
            row.addProperty("stored", stored.getRegisteredName()); row.addProperty("visible", visible.getRegisteredName());
            row.addProperty("unprojected", raw.getRegisteredName());
            row.addProperty("block", BuiltInRegistries.BLOCK.getKey(chunk.getBlockState(p).getBlock()).toString());
            row.addProperty("coldAtActualY", stored.value().coldEnoughToSnow(p));
            row.addProperty("coldAtReferenceY", stored.value().coldEnoughToSnow(IslandClimate.temperaturePosition(p)));
            row.addProperty("snowBlocks", snowBlocks); row.addProperty("snowLayers", snowLayers);
            row.addProperty("weatherBiome", level.getBiome(top).getRegisteredName());
            row.addProperty("weatherCold", level.getBiome(top).value().coldEnoughToSnow(IslandClimate.temperaturePosition(top)));
            row.addProperty("weatherCanSnow", level.getBiome(top).value().shouldSnow(level, top));
            counts.merge(stored.getRegisteredName(), 1, Integer::sum);
            tags.computeIfAbsent(stored.getRegisteredName(), ignored -> stored.tags().map(tag -> tag.location().toString()).sorted().toList());
            columns.add(row);
        }
        result.add("biomes", new Gson().toJsonTree(counts)); result.add("tags", new Gson().toJsonTree(tags)); result.add("columns", columns);
        return result;
    }
}
