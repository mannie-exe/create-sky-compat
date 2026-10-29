package design.inherent.createskycompat.mixin.client;

import net.irisshaders.iris.api.v0.IrisApi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A screen-space hot-air composite must not write into Iris's shadow pass. */
@Mixin(targets = "dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.effect.ClientBalloonEffectRenderer", remap = false)
public abstract class BalloonShadowMixin {
    @Inject(method = "onRenderLevelStage", at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private static void createSky$skipShadowComposite(CallbackInfo callback) {
        if (IrisApi.getInstance().isRenderingShadowPass()) callback.cancel();
    }
}
