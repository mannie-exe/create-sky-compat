package design.inherent.createskycompat.probe;

import com.google.gson.*;
import design.inherent.createskycompat.terrain.GeologicalGenerator;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;
import org.sathrek.sky_archipelago.worldgen.generator.core.SkyIslandChunkGenerator;
import org.sathrek.sky_archipelago.worldgen.generator.field.IslandField;
import java.nio.file.*;
import java.util.Comparator;
import java.util.concurrent.*;

/** Exercise the installed DH server generator in a disposable world, never the playing server. */
final class DhPregenProbe {
    private DhPregenProbe() {}

    static void start(MinecraftServer server, Path output) throws Exception {
        var level = server.overworld();
        var generator = (SkyIslandChunkGenerator)level.getChunkSource().getGenerator();
        var field = ((GeologicalGenerator)(Object)generator).createSky$field(level.getChunkSource().randomState());
        var anchor = field.collectIslandPreviewsInRadius(0, 0, 8000, generator.resolvedSkyIslandSettings())
                .stream().filter(p -> p.family() == IslandField.IslandFamily.ANCHOR_PLATEAU)
                .min(Comparator.comparingDouble(p -> Math.hypot(p.x(), p.z()))).orElseThrow();
        var shared = Class.forName("com.seibel.distanthorizons.core.api.internal.SharedApi");
        var world = shared.getMethod("getAbstractDhWorld").invoke(null);
        var manager = world.getClass().getMethod("getPregenManager").invoke(world);
        var wrapper = Class.forName("com.seibel.distanthorizons.common.wrappers.world.ServerLevelWrapper_neoforge")
                .getMethod("getWrapper", ServerLevel.class).invoke(null, level);
        var positionType = Class.forName("com.seibel.distanthorizons.core.pos.blockPos.DhBlockPos2D");
        var position = positionType.getConstructor(int.class, int.class).newInstance(anchor.x(), anchor.z());
        var levelType = Class.forName("com.seibel.distanthorizons.core.wrapperInterfaces.world.IServerLevelWrapper");
        server.saveEverything(false, true, true);
        var region = server.getWorldPath(LevelResource.ROOT).resolve("region");
        long before = storedChunks(region), began = System.nanoTime();
        // The installed command's minimum is 32 chunks; keep that same bounded workload.
        var future = (CompletableFuture<?>)manager.getClass().getMethod("startPregen", levelType, positionType, int.class)
                .invoke(manager, wrapper, position, 32);
        future.orTimeout(480, TimeUnit.SECONDS).whenComplete((ignored, failure) -> server.execute(() -> {
            var report = new JsonObject();
            report.addProperty("seed", Long.toString(level.getSeed()));
            report.addProperty("centerX", anchor.x()); report.addProperty("centerZ", anchor.z());
            report.addProperty("chunkRadius", 32);
            report.addProperty("storedChunksBefore", before);
            report.addProperty("elapsedSeconds", (System.nanoTime() - began) / 1e9);
            try {
                server.saveEverything(false, true, true);
                long after = storedChunks(region);
                report.addProperty("storedChunksAfter", after);
                report.addProperty("complete", failure == null && after > before);
                if (failure != null) report.addProperty("failure", failure.toString());
                Files.writeString(output, new GsonBuilder().setPrettyPrinting().create().toJson(report));
            } catch (Exception error) { throw new RuntimeException("DH probe could not save its evidence", error); }
            finally { server.halt(false); }
        }));
    }

    private static long storedChunks(Path region) throws Exception {
        if (!Files.isDirectory(region)) return 0;
        long count = 0;
        try (var files = Files.list(region)) {
            for (var path : files.filter(p -> p.toString().endsWith(".mca")).toList()) {
                try (var input = Files.newInputStream(path)) {
                    byte[] header = input.readNBytes(4096);
                    for (int i = 0; i + 3 < header.length; i += 4)
                        if (header[i] != 0 || header[i + 1] != 0 || header[i + 2] != 0) count++;
                }
            }
        }
        return count;
    }
}
