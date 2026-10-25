package design.inherent.createskycompat.placement;

import design.inherent.createskycompat.terrain.GeologicalGenerator;
import design.inherent.createskycompat.OptionalIntegrations;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.status.ChunkStatus;

/** Bounded retry inside a known Sky Village after its native delivery point fails. */
public final class VillageDeliveryLanding {
    private VillageDeliveryLanding() {}
    @FunctionalInterface
    public interface SurfaceResolver { BlockPos resolve(ServerLevel level, int x, int z); }

    public static BlockPos find(ServerLevel level, int x, int z, SurfaceResolver nativeSurface) {
        if (!OptionalIntegrations.skyVillages()) return null;
        if (!(level.getChunkSource().getGenerator() instanceof GeologicalGenerator generator)
                || !generator.createSky$isEnabled()) return null;
        var chunk = level.getChunkSource().getChunk(x >> 4, z >> 4, ChunkStatus.FULL, false);
        if (chunk == null) {
            LogUtils.getLogger().debug("Delivery retry skipped at {}, {}: start chunk is not loaded", x, z);
            return null;
        }
        var village = level.registryAccess().registryOrThrow(Registries.STRUCTURE)
                .get(ResourceLocation.parse("skyvillages:skyvillage"));
        if (village == null) return null;
        var start = chunk.getStartForStructure(village);
        if (start == null || !start.isValid()) {
            LogUtils.getLogger().debug("Delivery retry skipped at {}, {}: no valid Sky Village start", x, z);
            return null;
        }
        var bounds = start.getBoundingBox();
        for (int radius = 1; radius <= 8; radius++) {
            for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                int px = x + dx, pz = z + dz;
                if (px < bounds.minX() + 1 || px >= bounds.maxX() || pz < bounds.minZ() + 1 || pz >= bounds.maxZ()) continue;
                var result = nativeSurface.resolve(level, px, pz);
                if (result != null && bounds.isInside(result.below())) {
                    LogUtils.getLogger().debug("Delivery retry at {}, {} selected {}", x, z, result);
                    return result;
                }
            }
        }
        LogUtils.getLogger().debug("Delivery retry found no surface within eight blocks of {}, {}; village bounds {}", x, z, bounds);
        return null;
    }
}
