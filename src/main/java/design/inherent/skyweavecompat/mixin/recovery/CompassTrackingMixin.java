package design.inherent.skyweavecompat.mixin.recovery;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.data.DeathInfoManager;
import design.inherent.skyweavecompat.recovery.ShipGraves;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.CompassItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CompassItem.class)
public abstract class CompassTrackingMixin {
    @Inject(method = "inventoryTick", at = @At("TAIL"))
    private void skyweave$followConstruct(ItemStack stack, Level level, Entity holder, int slot,
            boolean selected, CallbackInfo ci) {
        if (level.isClientSide || level.getGameTime() % 20 != 0) return;
        var id = stack.get(Yigd.GRAVE_ID);
        if (id == null) return;
        DeathInfoManager.INSTANCE.getGrave(id).filter(grave -> ShipGraves.anchor(grave) != null)
                .ifPresent(grave -> stack.set(Yigd.GRAVE_LOCATION,
                        GlobalPos.of(grave.getWorldRegistryKey(), ShipGraves.worldPosition(grave))));
    }
}
