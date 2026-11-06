package design.inherent.skyweavecompat.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import design.inherent.skyweavecompat.Feature;
import design.inherent.skyweavecompat.OptionalIntegrations;
import design.inherent.skyweavecompat.client.ConstructProgram;
import design.inherent.skyweavecompat.client.ConstructUniforms;
import design.inherent.skyweavecompat.client.ConstructOcclusion;
import net.irisshaders.iris.gl.image.ImageHolder;
import net.irisshaders.iris.gl.sampler.SamplerHolder;
import net.minecraft.client.renderer.ShaderInstance;
import java.util.function.BiConsumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.irisshaders.iris.pipeline.programs.ExtendedShader", remap = false)
abstract class ConstructProgramMixin implements ConstructProgram {
    @Unique private ConstructUniforms skyweave$uniforms;
    @Unique private boolean skyweave$depthSampler;
    @Override public ConstructUniforms skyweave$constructUniforms() {
        if (skyweave$uniforms == null)
            skyweave$uniforms = new ConstructUniforms(((ShaderInstance)(Object)this).getId(), skyweave$depthSampler);
        return skyweave$uniforms;
    }
    @WrapOperation(method = "<init>", at = @At(value = "INVOKE",
            target = "Ljava/util/function/BiConsumer;accept(Ljava/lang/Object;Ljava/lang/Object;)V"), require = 1)
    private void skyweave$opaqueSampler(BiConsumer<SamplerHolder, ImageHolder> factory, Object samplers,
            Object images, Operation<Void> original) {
        original.call(factory, samplers, images);
        if (OptionalIntegrations.enabled(Feature.CONSTRUCT_OCCLUSION))
            skyweave$depthSampler = ConstructOcclusion.samplers((SamplerHolder)samplers);
    }
}
