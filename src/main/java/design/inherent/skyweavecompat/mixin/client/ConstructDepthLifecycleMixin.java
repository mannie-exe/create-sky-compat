package design.inherent.skyweavecompat.mixin.client;

import design.inherent.skyweavecompat.client.ConstructOcclusion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.irisshaders.iris.pipeline.IrisRenderingPipeline", remap = false)
abstract class ConstructDepthLifecycleMixin {
    @Inject(method = "destroy", at = @At("HEAD"), require = 1)
    private void skyweave$forgetDepth(CallbackInfo ci) { ConstructOcclusion.clear(this); }
}
