package design.inherent.createskycompat.probe;

import com.google.gson.*;
import design.inherent.createskycompat.terrain.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import org.sathrek.sky_archipelago.worldgen.generator.core.SkyIslandChunkGenerator;
import org.sathrek.sky_archipelago.worldgen.generator.field.IslandField;
import java.nio.file.*;
import java.util.*;

/** Target native cave-biome footprints before inspecting stored biomes and actual decoration. */
final class BiomeProbe {
    private BiomeProbe() {}
    static void run(MinecraftServer server, Path output) throws Exception {
        var level = server.overworld();
        var generator = (SkyIslandChunkGenerator)level.getChunkSource().getGenerator();
        var random = level.getChunkSource().randomState();
        var settings = generator.resolvedSkyIslandSettings();
        var field = ((GeologicalGenerator)(Object)generator).createSky$field(random);
        var targets = Set.of("minecraft:lush_caves", "minecraft:dripstone_caves", "minecraft:deep_dark");
        var found = new LinkedHashMap<String, BlockPos>();
        var anchors = field.collectIslandPreviewsInRadius(0, 0, 16000, settings).stream()
                .filter(p -> p.family() == IslandField.IslandFamily.ANCHOR_PLATEAU)
                .sorted(Comparator.comparingDouble(p -> Math.hypot(p.x(), p.z()))).toList();
        outer: for (var anchor : anchors) for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            int x = ((anchor.x() + dx * anchor.radius() / 2) >> 2) * 4 + 2;
            int z = ((anchor.z() + dz * anchor.radius() / 2) >> 2) * 4 + 2;
            var column = field.sampleColumnProfile(x, z, settings).terrainColumn();
            var frame = ((GeologicalField)(Object)field).createSky$frameAt(x, z, settings);
            if (frame == null || column.thickness() < 150) continue;
            // Deep Dark retains upstream's fixed -48 proxy and lower-45%-of-column rule.
            // Lush/dripstone use our geological inverse; those are different projection contracts.
            var dominant = field.sampleDominantIslandDescriptor(x, z, settings);
            if (dominant != null && dominant.family() == IslandField.IslandFamily.ANCHOR_PLATEAU
                    && generator.getBiomeSource().getNoiseBiome(x >> 2, -48 >> 2, z >> 2, random.sampler())
                    .getRegisteredName().equals("minecraft:deep_dark")) {
                int deepY = ((column.bottomY() + column.thickness() / 4) >> 2) * 4 + 2;
                found.putIfAbsent("minecraft:deep_dark", new BlockPos(x, deepY, z));
            }
            for (int referenceY : new int[]{-48, -16, 16, 48}) {
                int y = (frame.toWorld(referenceY) >> 2) * 4 + 2;
                if (y < column.bottomY() || y > column.topY() - 16) continue;
                var biome = generator.getBiomeSource().getNoiseBiome(x >> 2, frame.toReference(y) >> 2, z >> 2, random.sampler());
                String id = biome.getRegisteredName();
                if (targets.contains(id) && !id.equals("minecraft:deep_dark")) found.putIfAbsent(id, new BlockPos(x, y, z));
                if (found.size() == targets.size()) break outer;
            }
        }
        var rows = new JsonArray(); var pos = new BlockPos.MutableBlockPos();
        for (var entry : found.entrySet()) {
            var point = entry.getValue();
            var chunk = level.getChunk(point.getX() >> 4, point.getZ() >> 4);
            var row = new JsonObject(); row.addProperty("target", entry.getKey()); row.add("position", new Gson().toJsonTree(point));
            row.addProperty("unprojected", generator.getBiomeSource().getNoiseBiome(point.getX() >> 2, point.getY() >> 2,
                    point.getZ() >> 2, random.sampler()).getRegisteredName());
            row.addProperty("stored", chunk.getNoiseBiome(point.getX() >> 2, point.getY() >> 2, point.getZ() >> 2).getRegisteredName());
            int surface = field.sampleColumnProfile(point.getX(), point.getZ(), settings).topY();
            row.addProperty("surfaceUnchanged", chunk.getNoiseBiome(point.getX() >> 2, surface >> 2, point.getZ() >> 2)
                    .equals(generator.getBiomeSource().getNoiseBiome(point.getX() >> 2, surface >> 2, point.getZ() >> 2, random.sampler())));
            var blocks = new TreeMap<String, Integer>(); var caveBiomes = new TreeMap<String, Integer>();
            for (int x = chunk.getPos().getMinBlockX(); x <= chunk.getPos().getMaxBlockX(); x++)
                for (int z = chunk.getPos().getMinBlockZ(); z <= chunk.getPos().getMaxBlockZ(); z++)
                    for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) {
                        String id = BuiltInRegistries.BLOCK.getKey(chunk.getBlockState(pos.set(x, y, z)).getBlock()).toString();
                        if (id.contains("moss") || id.contains("dripstone") || id.contains("sculk") || id.contains("cave_vine")
                                || id.contains("spore_blossom") || id.contains("azalea") || id.equals("minecraft:clay")) blocks.merge(id, 1, Integer::sum);
                        if ((x & 3) == 2 && (y & 3) == 2 && (z & 3) == 2) {
                            String biome = chunk.getNoiseBiome(x >> 2, y >> 2, z >> 2).getRegisteredName();
                            if (targets.contains(biome)) caveBiomes.merge(biome, 1, Integer::sum);
                        }
                    }
            row.add("blocks", new Gson().toJsonTree(blocks)); row.add("caveBiomeCells", new Gson().toJsonTree(caveBiomes)); rows.add(row);
        }
        var report = new JsonObject(); report.addProperty("seed", level.getSeed()); report.add("samples", rows);
        var features = FeatureProbe.run(level);
        report.add("featureChecks", features);
        report.addProperty("targetsFound", found.size());
        boolean matches = rows.asList().stream().allMatch(value -> {
            var row = value.getAsJsonObject();
            return row.get("target").equals(row.get("stored")) && row.get("surfaceUnchanged").getAsBoolean();
        });
        for (String key : new String[]{"temperatePortal", "coldPortal"}) {
            if (!features.has(key)) { matches = false; continue; }
            var portal = features.getAsJsonObject(key);
            matches &= portal.get("cold").equals(portal.get("expectedCold"));
        }
        report.addProperty("complete", found.size() == targets.size() && matches);
        Files.writeString(output, new GsonBuilder().setPrettyPrinting().create().toJson(report));
        if (found.size() != targets.size()) throw new AssertionError("Could not find all cave-biome fixtures");
        if (!matches) throw new AssertionError("Cave biome projection or surface preservation failed");
    }
}
