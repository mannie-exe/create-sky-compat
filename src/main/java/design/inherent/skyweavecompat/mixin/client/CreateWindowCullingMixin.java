package design.inherent.skyweavecompat.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Different window materials need their own visible face and native CT context. */
@Mixin(targets = {
        "com.simibubi.create.content.decoration.palettes.ConnectedGlassBlock",
        "com.simibubi.create.content.decoration.palettes.WindowBlock"
}, remap = false)
abstract class CreateWindowCullingMixin {
    @ModifyReturnValue(method = "skipRendering", at = @At("RETURN"), require = 1, allow = 3)
    private boolean skyweave$preserveMaterialBoundary(boolean hidden, BlockState state,
            BlockState adjacent, Direction side) {
        return hidden && state.getBlock() == adjacent.getBlock();
    }
}
