package design.inherent.skyweavecompat.mixin.linkage;

import design.inherent.skyweavecompat.linkage.LinkPermissions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = {"com.enxv.aerouniversaljoint.content.HydraulicConnectionHeadBlock",
        "com.enxv.aerouniversaljoint.content.UniversalJointBlock"}, remap = false)
public abstract class LinkageUseMixin {
    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private void skyweave$authorize(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit,
                                    CallbackInfoReturnable<InteractionResult> cir) {
        if (player.isShiftKeyDown()) {
            cir.setReturnValue(InteractionResult.PASS);
        } else if (!level.isClientSide && !LinkPermissions.canModify(player, level.getBlockEntity(pos), false)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
