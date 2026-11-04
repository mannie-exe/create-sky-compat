package design.inherent.skyweavecompat.mixin.linear;

import design.inherent.skyweavecompat.linear.LinearAssembly;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.bearing.linearbearing.blocks.LinearBearingBlock", remap = false)
public abstract class LinearAssemblyMixin {
    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
    private void skyweave$checkedAssembly(BlockState state, Level level, BlockPos position, Player player,
            BlockHitResult hit, CallbackInfoReturnable<InteractionResult> callback) {
        callback.setReturnValue(!player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty() ? InteractionResult.PASS
                : level instanceof ServerLevel server ? LinearAssembly.assemble(server, position, state, player)
                : InteractionResult.SUCCESS);
    }
}
