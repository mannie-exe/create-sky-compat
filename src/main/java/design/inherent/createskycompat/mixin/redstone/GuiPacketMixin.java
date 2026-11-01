package design.inherent.createskycompat.mixin.redstone;

import design.inherent.createskycompat.redstone.GuiPermissions;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Runs inside each enqueued server task, before any world or menu mutation. */
@Mixin(targets = {
    "com.ggrgg.createredstonelinkgui.common.network.RedstoneLinkFrequencyPayload",
    "com.ggrgg.createredstonelinkgui.common.network.RedstoneLinkModeTogglePayload",
    "com.ggrgg.createredstonelinkgui.common.network.VectorThrusterFrequencyPayload",
    "com.ggrgg.createredstonelinkgui.common.network.CopyToPresetPayload",
    "com.ggrgg.createredstonelinkgui.common.network.PasteFromPresetPayload",
    "com.ggrgg.createredstonelinkgui.common.network.PresetSlotUpdatePayload",
    "com.ggrgg.createredstonelinkgui.common.network.VoidLinkClaimPayload",
    "com.ggrgg.createredstonelinkgui.common.network.TinyLinkFreqUpdatePayload"
}, remap = false)
public abstract class GuiPacketMixin {
    @Inject(method = "lambda$handleServer$0", at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private static void createSky$authorize(IPayloadContext context, @Coerce Object payload, CallbackInfo ci) {
        if (!GuiPermissions.packet(context.player(), payload)) ci.cancel();
    }
}
