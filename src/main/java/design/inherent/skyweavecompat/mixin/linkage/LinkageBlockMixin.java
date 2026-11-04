package design.inherent.skyweavecompat.mixin.linkage;

import design.inherent.skyweavecompat.linkage.LinkPermissions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = {"com.enxv.aerouniversaljoint.content.HydraulicConnectionHeadBlock",
        "com.enxv.aerouniversaljoint.content.UniversalJointBlock",
        "com.enxv.aerouniversaljoint.content.DampingStressBearingBlock"}, remap = false)
public abstract class LinkageBlockMixin {
    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private void skyweave$authorize(ItemStack held, BlockState state, Level level, BlockPos pos,
                                    Player player, InteractionHand hand, BlockHitResult hit,
                                    CallbackInfoReturnable<ItemInteractionResult> cir) {
        if (!level.isClientSide && !LinkPermissions.canModify(player, level.getBlockEntity(pos), false))
            cir.setReturnValue(ItemInteractionResult.FAIL);
    }
}
