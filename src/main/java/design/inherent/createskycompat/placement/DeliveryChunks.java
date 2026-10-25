package design.inherent.createskycompat.placement;

import com.mojang.logging.LogUtils;
import design.inherent.createskycompat.terrain.GeologicalGenerator;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import java.util.*;
import java.util.concurrent.*;

/** Hold two bounded endpoint neighborhoods until ADQ registers or rejects its route. */
public final class DeliveryChunks {
    private static final TicketType<UUID> TICKET = TicketType.create("create_sky_delivery", UUID::compareTo);
    // ADQ 1.1.1 admits one generation job at a time. Mutations stay on the server thread.
    private static final Map<ServerLevel, Lease> ACTIVE = new HashMap<>();
    private DeliveryChunks() {}

    public static boolean applies(ServerChunkCache cache, ChunkStatus status, boolean create) {
        return create && status == ChunkStatus.FULL && applies((ServerLevel)cache.getLevel());
    }

    public static boolean applies(ServerLevel level) {
        return level.getChunkSource().getGenerator() instanceof GeologicalGenerator generator && generator.createSky$isEnabled();
    }

    public static CompletableFuture<ChunkResult<ChunkAccess>> request(ServerChunkCache cache, int x, int z) {
        var level = (ServerLevel)cache.getLevel(); var server = level.getServer();
        return server.submit(() -> {
            var lease = ACTIVE.computeIfAbsent(level, Lease::new);
            try { lease.acquire(x, z); return lease; }
            catch (Throwable failure) { lease.close(); throw failure; }
        }).thenCompose(lease -> CompletableFuture.supplyAsync(() -> {
            var futures = new ArrayList<CompletableFuture<ChunkResult<ChunkAccess>>>();
            // Nine chunks cover the native 3x3-block predicate and the eight-block fallback.
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++)
                futures.add(cache.getChunkFuture(x + dx, z + dz, ChunkStatus.FULL, true));
            return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenApply(unused -> {
                for (var future : futures) {
                    var result = future.join();
                    if (!result.isSuccess()) throw new CompletionException(new IllegalStateException(result.getError()));
                }
                return futures.get(4).join();
            });
        }).thenCompose(future -> future).orTimeout(60, TimeUnit.SECONDS).whenComplete((result, failure) -> {
            if (failure != null) server.execute(lease::close);
        }));
    }

    public static void finish(ServerLevel level) {
        var lease = ACTIVE.get(level);
        if (lease != null) lease.close();
    }

    public static void stop(MinecraftServer server) {
        for (var lease : List.copyOf(ACTIVE.values()))
            if (lease.level.getServer() == server) lease.close();
    }

    private static final class Lease {
        private final ServerLevel level;
        private final UUID id = UUID.randomUUID();
        private final Set<ChunkPos> chunks = new HashSet<>();
        private boolean closed;
        private Lease(ServerLevel level) { this.level = level; }

        private void acquire(int x, int z) {
            if (closed) throw new IllegalStateException("Delivery chunk lease already closed");
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                var pos = new ChunkPos(x + dx, z + dz);
                if (chunks.contains(pos)) continue;
                if (chunks.size() >= 18) throw new IllegalStateException("Delivery endpoint chunk bound exceeded");
                level.getChunkSource().addRegionTicket(TICKET, pos, 0, id);
                chunks.add(pos);
            }
        }

        private void close() {
            if (closed) return;
            closed = true;
            for (var pos : chunks) level.getChunkSource().removeRegionTicket(TICKET, pos, 0, id);
            LogUtils.getLogger().debug("Released {} delivery chunk tickets", chunks.size());
            ACTIVE.remove(level, this);
        }
    }
}
