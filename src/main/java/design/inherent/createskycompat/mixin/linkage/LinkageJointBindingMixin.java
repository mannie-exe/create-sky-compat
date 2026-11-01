package design.inherent.createskycompat.mixin.linkage;

import com.enxv.aerouniversaljoint.content.UniversalJointBlockEntity;
import com.enxv.aerouniversaljoint.content.JointBindingData;
import com.enxv.aerouniversaljoint.util.SubLevelReferenceHelper;
import com.llamalad7.mixinextras.sugar.Local;
import design.inherent.createskycompat.linkage.LinkPermissions;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = UniversalJointBlockEntity.class, remap = false)
public abstract class LinkageJointBindingMixin {
    @Inject(method = "handleHeldRodItem", at = @At(value = "INVOKE", target =
            "Lcom/enxv/aerouniversaljoint/content/UniversalJointBlockEntity;linkToSelection(Lcom/enxv/aerouniversaljoint/content/JointBindingData$Selection;Z)Lcom/enxv/aerouniversaljoint/content/UniversalJointBlockEntity$LinkResult;"),
            cancellable = true, require = 1, allow = 1)
    private void createSky$authorizeBinding(CallbackInfoReturnable<ItemInteractionResult> cir,
                                           @Local(argsOnly = true) Player player,
                                           @Local(ordinal = 1) JointBindingData.Selection selection) {
        var own = (UniversalJointBlockEntity)(Object)this;
        var peer = SubLevelReferenceHelper.resolveBlockEntityFast(player.level(), selection.pos(), selection.subLevelId());
        if (!LinkPermissions.canModify(player, own, false) || !LinkPermissions.canModify(player, peer, false))
            cir.setReturnValue(ItemInteractionResult.FAIL);
    }
}
