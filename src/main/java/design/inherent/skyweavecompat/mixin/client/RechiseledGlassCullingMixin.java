package design.inherent.skyweavecompat.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Vertical pillars must respect the same orientation boundary as rotated pillars. */
@Mixin(targets = "com.supermartijn642.rechiseled.blocks.RechiseledGlassPillarBlock", remap = false)
abstract class RechiseledGlassCullingMixin {
    @ModifyReturnValue(method = "skipRendering", at = @At("RETURN"), require = 1, allow = 3)
    private boolean skyweave$preserveRotatedBoundary(boolean hidden, BlockState state,
            BlockState adjacent, Direction side) {
        return hidden && (state.getBlock() != adjacent.getBlock()
                || state.getValue(BlockStateProperties.AXIS) == adjacent.getValue(BlockStateProperties.AXIS));
    }
}
