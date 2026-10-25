package design.inherent.createskycompat.probe;

import com.google.gson.*;
import design.inherent.createskycompat.placement.DeliveryChunks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Compare ADQ-style background FULL requests with explicitly retained tickets. */
final class ChunkFutureProbe {
    private static final TicketType<ChunkPos> TICKET = TicketType.create("create_sky_probe", Comparator.comparingLong(ChunkPos::toLong));
    private static final List<ChunkPos> POSITIONS = List.of(new ChunkPos(6, 51), new ChunkPos(121, -20));
    private ChunkFutureProbe() {}

    static void start(MinecraftServer server, Path output) {
        var cache = server.overworld().getChunkSource();
        for (var pos : POSITIONS) cache.getChunk(pos.x, pos.z, ChunkStatus.STRUCTURE_STARTS, true);
        var rows = new JsonArray();
        trial(server, output, rows, 0);
    }

    private static void trial(MinecraftServer server, Path output, JsonArray rows, int mode) {
        var cache = server.overworld().getChunkSource();
        boolean retain = mode == 1;
        if (retain) for (var pos : POSITIONS) cache.addRegionTicket(TICKET, pos, 0, pos);
        long began = System.nanoTime();
        CompletableFuture.supplyAsync(() -> {
            var futures = POSITIONS.stream().map(pos -> mode == 2 ? DeliveryChunks.request(cache, pos.x, pos.z)
                    : cache.getChunkFuture(pos.x, pos.z, ChunkStatus.FULL, true)).toList();
            return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenApply(unused ->
                    futures.stream().map(CompletableFuture::join).toList());
        }).thenCompose(future -> future).orTimeout(30, TimeUnit.SECONDS).whenComplete((results, failure) -> server.execute(() -> {
            var row = new JsonObject(); row.addProperty("retainedTickets", retain);
            row.addProperty("productionAdapter", mode == 2);
            row.addProperty("seconds", (System.nanoTime() - began) / 1e9);
            boolean success = failure == null;
            if (failure != null) row.addProperty("failure", failure.toString());
            var chunks = new JsonArray();
            if (results != null) for (int i = 0; i < results.size(); i++) {
                var result = results.get(i); var pos = POSITIONS.get(i); var chunk = result.orElse(null);
                var item = new JsonObject(); item.addProperty("x", pos.x); item.addProperty("z", pos.z);
                item.addProperty("success", result.isSuccess());
                item.addProperty("error", result.getError());
                item.addProperty("status", chunk == null ? "absent" : chunk.getPersistedStatus().toString());
                item.addProperty("getChunkNow", cache.getChunkNow(pos.x, pos.z) != null);
                chunks.add(item); success &= result.isSuccess();
            }
            row.add("chunks", chunks); rows.add(row);
            if (retain) for (var pos : POSITIONS) cache.removeRegionTicket(TICKET, pos, 0, pos);
            if (mode < 2) { trial(server, output, rows, mode + 1); return; }
            try {
                var owner = Class.forName("com.ladderstar.adq.QuestGenerator");
                var fail = owner.getDeclaredMethod("failGeneration", net.minecraft.server.level.ServerLevel.class, UUID.class);
                fail.setAccessible(true); fail.invoke(null, server.overworld(), null);
                var active = DeliveryChunks.class.getDeclaredField("ACTIVE"); active.setAccessible(true);
                if (!((Map<?, ?>)active.get(null)).isEmpty()) throw new AssertionError("Rejected route retained chunk leases");
                row.addProperty("rejectionReleasesLease", true);
            } catch (Throwable error) {
                success = false; row.addProperty("cleanupFailure", error.toString()); error.printStackTrace();
            }
            var report = new JsonObject(); report.add("trials", rows); report.addProperty("complete", success);
            try { Files.writeString(output, new GsonBuilder().setPrettyPrinting().create().toJson(report)); }
            catch (Exception error) { error.printStackTrace(); }
            finally { server.halt(false); }
        }));
    }
}
