package design.inherent.createskycompat.terrain;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;

/** Startup-only validation after native fallback selection, never called by terrain generation. */
public final class SpawnLanding {
    private SpawnLanding() {}

    public static BlockPos resolve(ServerLevel level, BlockPos proposed) {
        // Generated surfaces can differ from predicted columns after carving and features.
        for (int radius = 0; radius <= 16; radius += 4) {
            for (int dx = -radius; dx <= radius; dx += 4) for (int dz = -radius; dz <= radius; dz += 4) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                int x = proposed.getX() + dx, z = proposed.getZ() + dz;
                var chunk = level.getChunk(x >> 4, z >> 4);
                var feet = new BlockPos(x, chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x & 15, z & 15) + 1, z);
                if (feet.getY() <= level.getMinBuildHeight() || feet.getY() + 1 >= level.getMaxBuildHeight()) continue;
                if (level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), Direction.UP)
                        && level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                        && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()
                        && level.getFluidState(feet).isEmpty() && level.getFluidState(feet.above()).isEmpty()) return feet;
            }
        }
        throw new IllegalStateException("No supported generated landing near fresh-world spawn " + proposed);
    }
}
