package design.inherent.createskycompat.macaw;

import com.mcwbridges.kikoz.objects.items.Bridge_Lantern;
import design.inherent.createskycompat.mixin.macaw.ClipModeAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Check only the neighboring owners whose hanging outlines enter a visited cell. */
public final class LanternSelection {
    public static BlockHitResult extend(BlockGetter getter, ClipContext context, BlockHitResult nativeHit) {
        if (((ClipModeAccessor) context).createSky$blockMode() != ClipContext.Block.OUTLINE
                || !(getter instanceof Level level) || context.getFrom().equals(context.getTo())) return nativeHit;
        var from = context.getFrom();
        var end = nativeHit.getType() == HitResult.Type.MISS ? context.getTo() : nativeHit.getLocation();
        var found = BlockGetter.traverseBlocks(from, end, context, (ray, cell) -> {
            BlockHitResult nearest = null;
            // Each rotated lantern overhangs one horizontal face and the cell below.
            for (int dy = 0; dy <= 1; dy++) {
                for (int side = 0; side < 5; side++) {
                    var owner = cell.offset(side == 1 ? -1 : side == 2 ? 1 : 0, dy,
                            side == 3 ? -1 : side == 4 ? 1 : 0);
                    if (!level.hasChunkAt(owner)) continue;
                    var state = level.getBlockState(owner);
                    if (!(state.getBlock() instanceof Bridge_Lantern)) continue;
                    var shape = LanternShapes.stair(state);
                    if (shape == null) continue;
                    var hit = shape.clip(from, context.getTo(), owner);
                    if (hit == null || !inCell(hit, cell)) continue;
                    double distance = from.distanceToSqr(hit.getLocation());
                    if (distance >= from.distanceToSqr(end)) continue;
                    if (nearest == null || distance < from.distanceToSqr(nearest.getLocation())) nearest = hit;
                }
            }
            return nearest;
        }, ray -> null);
        return found == null ? nativeHit : found;
    }

    private static boolean inCell(BlockHitResult hit, BlockPos cell) {
        var p = hit.getLocation();
        double epsilon = 1e-7;
        return p.x >= cell.getX() - epsilon && p.x <= cell.getX() + 1 + epsilon
                && p.y >= cell.getY() - epsilon && p.y <= cell.getY() + 1 + epsilon
                && p.z >= cell.getZ() - epsilon && p.z <= cell.getZ() + 1 + epsilon;
    }
    private LanternSelection() {}
}
