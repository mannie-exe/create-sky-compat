package design.inherent.skyweavecompat.mixin.linkage;

import com.enxv.aerouniversaljoint.content.HydraulicConnectionHeadBlockEntity;
import com.enxv.aerouniversaljoint.content.JointBindingData;
import com.enxv.aerouniversaljoint.util.SubLevelReferenceHelper;
import com.llamalad7.mixinextras.sugar.Local;
import design.inherent.skyweavecompat.linkage.LinkPermissions;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Validate the resolved selection after upstream's move remapping, before either endpoint changes. */
@Mixin(value = HydraulicConnectionHeadBlockEntity.class, remap = false)
public abstract class LinkageBindingMixin {
    @Inject(method = "handleHeldHydraulicRodItem", at = @At(value = "INVOKE", target =
            "Lcom/enxv/aerouniversaljoint/content/HydraulicConnectionHeadBlockEntity;linkToSelection(Lcom/enxv/aerouniversaljoint/content/JointBindingData$Selection;ZZ)Lcom/enxv/aerouniversaljoint/content/HydraulicConnectionHeadBlockEntity$LinkResult;"),
            cancellable = true, require = 1, allow = 1)
    private void skyweave$authorizeBinding(CallbackInfoReturnable<ItemInteractionResult> cir,
                                           @Local(argsOnly = true) Player player,
                                           @Local(ordinal = 1) JointBindingData.Selection selection) {
        var own = (HydraulicConnectionHeadBlockEntity)(Object)this;
        var peer = SubLevelReferenceHelper.resolveBlockEntityFast(player.level(), selection.pos(), selection.subLevelId());
        if (!LinkPermissions.canModify(player, own, false) || !LinkPermissions.canModify(player, peer, false))
            cir.setReturnValue(ItemInteractionResult.FAIL);
    }
}
