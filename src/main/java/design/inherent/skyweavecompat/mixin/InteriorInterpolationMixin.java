package design.inherent.skyweavecompat.mixin;

import design.inherent.skyweavecompat.terrain.LayeredColumn;
import org.sathrek.sky_archipelago.worldgen.WorldgenPerformanceMetrics;
import org.sathrek.sky_archipelago.worldgen.generator.field.internal.ChunkTerrainSnapshotResolver;
import org.sathrek.sky_archipelago.worldgen.generator.field.model.ColumnData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Four exterior heights cannot prove the absence of a passage between them. */
@Mixin(value = ChunkTerrainSnapshotResolver.class, remap = false)
public abstract class InteriorInterpolationMixin {
    @Inject(method = "interpolationResolution", at = @At("HEAD"), cancellable = true)
    private static void interior(ColumnData a, ColumnData b, ColumnData c, ColumnData d,
            CallbackInfoReturnable<WorldgenPerformanceMetrics.GridCellResolution> result) {
        for (var corner : new ColumnData[]{a, b, c, d}) {
            if (corner != null && ((LayeredColumn)(Object)corner).skyweave$requiresExactInterior()) {
                result.setReturnValue(WorldgenPerformanceMetrics.GridCellResolution.MULTI_SEGMENT);
                return;
            }
        }
    }
}
