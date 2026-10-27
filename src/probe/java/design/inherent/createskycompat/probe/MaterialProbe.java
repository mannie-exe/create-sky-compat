package design.inherent.createskycompat.probe;

import com.google.gson.*;
import design.inherent.createskycompat.terrain.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Blocks;
import org.sathrek.sky_archipelago.worldgen.generator.core.SkyIslandChunkGenerator;
import java.nio.file.*;

/** Find a native mangrove surface and inspect completed chunk material. */
final class MaterialProbe {
    private MaterialProbe() {}
    static void run(MinecraftServer server, Path output) throws Exception {
        var level = server.overworld();
        var generator = (SkyIslandChunkGenerator)level.getChunkSource().getGenerator();
        var random = level.getChunkSource().randomState();
        var field = ((GeologicalGenerator)(Object)generator).createSky$field(random);
        var settings = generator.resolvedSkyIslandSettings();
        var rows = new JsonArray();
        int candidates = 0;
        search: for (int cx = -16; cx <= 16; cx++) for (int cz = -16; cz <= 16; cz++) {
            var members = ((ClusterField)(Object)field).createSky$members(cx, cz, settings);
            if (members.isEmpty()) continue;
            var anchor = members.getFirst();
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                int x = anchor.centerX() + dx * anchor.radiusX() / 2;
                int z = anchor.centerZ() + dz * anchor.radiusZ() / 2;
                var predictedBiome = generator.getBiomeSource().getNoiseBiome(x >> 2,
                        (anchor.centerY() + anchor.plateauHeight()) >> 2, z >> 2, random.sampler());
                if (!predictedBiome.is(BiomeMaterials.MUD)) continue;
                candidates++;
                var profile = field.sampleColumnProfile(x, z, settings);
                if (!profile.exists() || ((InteriorField)(Object)field).createSky$waterAt(x, z, settings).contains(profile.topY())) continue;
                var chunk = level.getChunk(x >> 4, z >> 4);
                int mud = 0, mudBiomes = 0;
                var pos = new BlockPos.MutableBlockPos();
                for (int bx = chunk.getPos().getMinBlockX(); bx <= chunk.getPos().getMaxBlockX(); bx++)
                    for (int bz = chunk.getPos().getMinBlockZ(); bz <= chunk.getPos().getMaxBlockZ(); bz++) {
                        var column = field.sampleColumnProfile(bx, bz, settings);
                        if (!column.exists()) continue;
                        var biome = chunk.getNoiseBiome(bx >> 2, column.topY() >> 2, bz >> 2);
                        if (!biome.is(BiomeMaterials.MUD)) continue;
                        mudBiomes++;
                        for (int depth = 0; depth <= 2; depth++)
                            if (chunk.getBlockState(pos.set(bx, column.topY() - depth, bz)).is(Blocks.MUD)) mud++;
                    }
                var row = new JsonObject(); row.addProperty("x", x); row.addProperty("z", z);
                row.addProperty("biome", predictedBiome.getRegisteredName());
                row.addProperty("mudBiomeColumns", mudBiomes); row.addProperty("mudSurfaceBlocks", mud);
                rows.add(row);
                if (mud > 0) break search;
                if (rows.size() >= 4) break search;
            }
        }
        var report = new JsonObject(); report.addProperty("seed", Long.toString(level.getSeed()));
        report.addProperty("candidateColumns", candidates); report.add("samples", rows);
        boolean complete = rows.asList().stream().anyMatch(row -> row.getAsJsonObject().get("mudSurfaceBlocks").getAsInt() > 0);
        report.addProperty("complete", complete);
        Files.writeString(output, new GsonBuilder().setPrettyPrinting().create().toJson(report) + "\n");
        if (!complete) throw new AssertionError("No completed mangrove mud surface in bounded search");
    }
}
