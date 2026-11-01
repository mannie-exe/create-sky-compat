package design.inherent.createskycompat.mixin.linkage;

import design.inherent.createskycompat.linkage.LinkPermissions;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.enxv.aerouniversaljoint.content.WrenchRemovalHelper", remap = false)
public abstract class LinkageWrenchMixin {
    @Inject(method = "removeWithDrops", at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private static void createSky$authorize(BlockState state, UseOnContext context, Runnable beforeRemove,
                                            CallbackInfoReturnable<InteractionResult> cir) {
        if (!context.getLevel().isClientSide && !LinkPermissions.canRemoveBlock(context.getPlayer(),
                context.getLevel().getBlockEntity(context.getClickedPos())))
            cir.setReturnValue(InteractionResult.FAIL);
    }
}
