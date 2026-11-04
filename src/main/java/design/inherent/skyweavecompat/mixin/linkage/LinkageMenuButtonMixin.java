package design.inherent.skyweavecompat.mixin.linkage;

import com.enxv.aerouniversaljoint.content.DampingStressBearingMenu;
import design.inherent.skyweavecompat.linkage.LinkPermissions;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = DampingStressBearingMenu.class, remap = false)
public abstract class LinkageMenuButtonMixin {
    @Inject(method = "clickMenuButton", at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private void skyweave$authorize(Player player, int id, CallbackInfoReturnable<Boolean> cir) {
        var menu = (DampingStressBearingMenu)(Object)this;
        if (!player.level().isClientSide && (!menu.stillValid(player) || !LinkPermissions.canModify(player,
                player.level().getBlockEntity(menu.getBlockPos()), false))) cir.setReturnValue(false);
    }
}
