package design.inherent.createskycompat.terrain;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.sathrek.sky_archipelago.config.SkyIslandSettings;
import org.sathrek.sky_archipelago.worldgen.generator.field.IslandField;

/** Only native chunk filling schedules fluids. Neighbor checks remain pure column queries. */
public final class WaterPostprocessing {
    private WaterPostprocessing() {}
    public static void mark(ChunkAccess chunk, IslandField field, SkyIslandSettings settings) {
        if (((ProfiledTerrain)(Object)field).createSky$profile() == null) return;
        var waterField = (InteriorField)(Object)field;
        var pos = new BlockPos.MutableBlockPos();
        for (int x = chunk.getPos().getMinBlockX(); x <= chunk.getPos().getMaxBlockX(); x++)
            for (int z = chunk.getPos().getMinBlockZ(); z <= chunk.getPos().getMaxBlockZ(); z++) {
                var water = waterField.createSky$waterAt(x, z, settings);
                for (var range : water.sources()) for (int y = range.bottomY(); y <= range.topY(); y++) {
                    if (chunk.getBlockState(pos.set(x, y, z)).getFluidState().isEmpty()) continue;
                    for (var direction : Direction.values()) {
                        int nx = x + direction.getStepX(), ny = y + direction.getStepY(), nz = z + direction.getStepZ();
                        if (!waterField.createSky$waterAt(nx, nz, settings).contains(ny)
                                && field.sampleSolidSegments(nx, nz, settings).stream().noneMatch(s -> s.contains(ny))) {
                            chunk.markPosForPostprocessing(pos);
                            break;
                        }
                    }
                }
            }
    }
}
