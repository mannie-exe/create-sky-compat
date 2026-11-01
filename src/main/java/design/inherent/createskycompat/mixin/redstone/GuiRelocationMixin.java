package design.inherent.createskycompat.mixin.redstone;

import com.ggrgg.createredstonelinkgui.common.network.RedstoneLinkMovePayload;
import design.inherent.createskycompat.redstone.GuiRelocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RedstoneLinkMovePayload.class, remap = false)
public abstract class GuiRelocationMixin {
    @Inject(method = "lambda$handleServer$2", at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private static void createSky$move(IPayloadContext context, RedstoneLinkMovePayload payload, CallbackInfo ci) {
        if (!GuiRelocation.allow(context.player(), payload)) ci.cancel();
    }
}
