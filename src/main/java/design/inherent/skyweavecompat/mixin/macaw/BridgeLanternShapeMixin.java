package design.inherent.skyweavecompat.mixin.macaw;

import design.inherent.skyweavecompat.macaw.LanternShapes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.mcwbridges.kikoz.objects.items.Bridge_Lantern", remap = false)
abstract class BridgeLanternShapeMixin {
    @Inject(method = "getShape", at = @At("HEAD"), cancellable = true, require = 1)
    private void skyweave$bridgeOutline(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context, CallbackInfoReturnable<VoxelShape> cir) {
        var shape = LanternShapes.outline(state);
        if (shape != null) cir.setReturnValue(shape);
    }
}
