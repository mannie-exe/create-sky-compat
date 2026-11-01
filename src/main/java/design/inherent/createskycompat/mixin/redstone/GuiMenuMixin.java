package design.inherent.createskycompat.mixin.redstone;

import com.ggrgg.createredstonelinkgui.common.menu.AbstractLinkMenu;
import design.inherent.createskycompat.redstone.GuiMenuState;
import design.inherent.createskycompat.redstone.GuiPermissions;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AbstractLinkMenu.class, remap = false)
public abstract class GuiMenuMixin implements GuiMenuState {
    @Shadow @Final protected Player player;
    @Unique private BlockEntity createSky$target;
    @Unique private UUID createSky$frame;
    @Inject(method = "<init>", at = @At("RETURN"), require = 1, allow = 1)
    private void createSky$capture(int id, Inventory inventory, BlockPos pos, MenuType<?> type, CallbackInfo ci) {
        if (player.level().hasChunkAt(pos)) createSky$target = player.level().getBlockEntity(pos);
        createSky$frame = GuiPermissions.frame(player.level(), pos);
    }
    public BlockEntity createSky$initialTarget() { return createSky$target; }
    public Player createSky$owner() { return player; }
    public UUID createSky$frame() { return createSky$frame; }
    @Inject(method = "stillValid", at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private void createSky$valid(Player owner, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(GuiPermissions.menuValid((AbstractLinkMenu)(Object)this, owner));
    }
    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private void createSky$click(int slot, int button, ClickType click, Player owner, CallbackInfo ci) {
        if (!GuiPermissions.menuValid((AbstractLinkMenu)(Object)this, owner)) ci.cancel();
    }
}
