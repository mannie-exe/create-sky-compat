package design.inherent.skyweavecompat.mixin.linkage;

import design.inherent.skyweavecompat.linkage.LinkPermissions;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Run inside each upstream enqueued task, never on its network thread. */
@Mixin(targets = {
        "com.enxv.aerouniversaljoint.network.BreakHydraulicRodPayload",
        "com.enxv.aerouniversaljoint.network.OpenHydraulicRodSettingsPayload",
        "com.enxv.aerouniversaljoint.network.SetHydraulicSettingsPayload",
        "com.enxv.aerouniversaljoint.network.SetGiantHydraulicSettingsPayload",
        "com.enxv.aerouniversaljoint.network.SetHydraulicHingeLimitsPayload",
        "com.enxv.aerouniversaljoint.network.SetDampingResistancePayload",
        "com.enxv.aerouniversaljoint.network.SetJointSpeedRatioPayload"
}, remap = false)
public abstract class LinkagePacketMixin {
    @Inject(method = "lambda$handleServer$0", at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private static void skyweave$authorize(IPayloadContext context, @Coerce Object payload, CallbackInfo ci) {
        if (!LinkPermissions.packet(context.player(), payload)) ci.cancel();
    }
}
