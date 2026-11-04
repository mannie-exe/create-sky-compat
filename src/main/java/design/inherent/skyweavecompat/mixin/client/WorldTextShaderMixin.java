package design.inherent.skyweavecompat.mixin.client;

import design.inherent.skyweavecompat.client.WorldTextLighting;
import net.irisshaders.iris.pipeline.programs.ShaderKey;
import net.irisshaders.iris.pipeline.programs.ShaderMap;
import net.minecraft.client.renderer.ShaderInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ShaderMap.class, remap = false)
public abstract class WorldTextShaderMixin {
    @Inject(method = "getShader", at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private void skyweave$textShader(ShaderKey key, CallbackInfoReturnable<ShaderInstance> callback) {
        ShaderInstance shader = WorldTextLighting.shader((ShaderMap) (Object) this, key);
        if (shader != null) callback.setReturnValue(shader);
    }
}
