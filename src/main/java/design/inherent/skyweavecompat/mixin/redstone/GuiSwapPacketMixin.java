package design.inherent.skyweavecompat.mixin.redstone;

import design.inherent.skyweavecompat.redstone.GuiPermissions;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Runs inside each enqueued server task, before any world or menu mutation. */
@Mixin(targets = "com.ggrgg.createredstonelinkgui.common.network.TinyLinkScreenSwapPayload", remap = false)
public abstract class GuiSwapPacketMixin {
    @Inject(method = "lambda$handleServer$2", at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private static void skyweave$authorize(IPayloadContext context, @Coerce Object payload, CallbackInfo ci) {
        if (!GuiPermissions.packet(context.player(), payload)) ci.cancel();
    }
}
