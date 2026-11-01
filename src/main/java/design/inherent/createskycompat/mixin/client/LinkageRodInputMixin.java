package design.inherent.createskycompat.mixin.client;

import com.enxv.aerouniversaljoint.client.HydraulicRodTargeting;
import com.enxv.aerouniversaljoint.network.BreakHydraulicRodPayload;
import com.simibubi.create.content.equipment.wrench.WrenchItem;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Ordinary attacks keep their normal meaning; sneak-wrench use detaches a visible rod. */
@Mixin(value = HydraulicRodTargeting.class, remap = false)
public abstract class LinkageRodInputMixin {
    @Shadow private static boolean usePressConsumed;
    @Shadow private static com.enxv.aerouniversaljoint.content.HydraulicRodInteraction.Hit pick(Minecraft mc, float partialTicks) {
        throw new AssertionError();
    }

    @Inject(method = "onInteraction", at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private static void createSky$deliberateRemoval(InputEvent.InteractionKeyMappingTriggered event, CallbackInfo ci) {
        if (event.isAttack()) { ci.cancel(); return; }
        var mc = Minecraft.getInstance();
        if (!event.isUseItem() || mc.player == null || !mc.player.isShiftKeyDown()
                || !(mc.player.getMainHandItem().getItem() instanceof WrenchItem)) return;
        var hit = pick(mc, mc.getTimer().getGameTimeDeltaPartialTick(true));
        if (hit == null) return;
        if (!usePressConsumed) {
            usePressConsumed = true;
            PacketDistributor.sendToServer(new BreakHydraulicRodPayload(hit.pos(), hit.subLevelId()));
        }
        event.setCanceled(true);
        ci.cancel();
    }
}
