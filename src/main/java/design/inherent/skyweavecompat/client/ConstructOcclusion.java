package design.inherent.skyweavecompat.client;

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.enums.rendering.EDhApiRenderPass;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBeforeRenderPassEvent;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiEventParam;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiRenderParam;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.compat.dh.DHCompat;
import net.irisshaders.iris.gl.sampler.SamplerHolder;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.uniforms.SystemTimeUniforms;

/** DH alone owns depth textures. Only an opaque pass in this frame authorizes sampling them. */
public final class ConstructOcclusion {
    private static final OpaqueDepthFrame FRAME = new OpaqueDepthFrame();
    private ConstructOcclusion() {}

    public static void register() {
        DhApi.events.bind(DhApiBeforeRenderPassEvent.class, new DhApiBeforeRenderPassEvent() {
            @Override public void beforeRender(DhApiEventParam<DhApiRenderParam> event) {
                if (IrisApi.getInstance().isRenderingShadowPass()) return;
                Object pipeline = Iris.getPipelineManager().getPipeline().orElse(null);
                if (event.value.renderPass == EDhApiRenderPass.OPAQUE) {
                    FRAME.opaque(pipeline, SystemTimeUniforms.COUNTER.getAsInt());
                } else if (event.value.renderPass == EDhApiRenderPass.TRANSPARENT) {
                    FRAME.translucent(pipeline, SystemTimeUniforms.COUNTER.getAsInt());
                }
            }
        });
    }

    public static boolean ready() {
        var pipeline = Iris.getPipelineManager().getPipeline().orElse(null);
        return FRAME.valid(pipeline, SystemTimeUniforms.COUNTER.getAsInt())
                && pipeline instanceof IrisRenderingPipeline iris
                && iris.shouldOverrideShaders() && !IrisApi.getInstance().isRenderingShadowPass()
                && iris.getDHCompat().getInstance() instanceof net.irisshaders.iris.compat.dh.DHCompatInternal dh
                && dh.shouldOverride && texture() > 0 && separateDepthAttachment();
    }

    public static void clear(Object pipeline) { FRAME.clear(pipeline); }

    private static boolean separateDepthAttachment() {
        int fbo = org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        if (fbo == 0) return false;
        int attachment = org.lwjgl.opengl.GL30.glGetFramebufferAttachmentParameteri(
                org.lwjgl.opengl.GL30.GL_DRAW_FRAMEBUFFER, org.lwjgl.opengl.GL30.GL_DEPTH_ATTACHMENT,
                org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME);
        return attachment != texture();
    }

    public static org.joml.Matrix4f inverse() { return DHCompat.getProjection().invert(); }

    private static int texture() {
        var pipeline = Iris.getPipelineManager().getPipeline().orElse(null);
        if (pipeline == null) return 0;
        var dh = pipeline.getDHCompat();
        return FRAME.snapshot() ? dh.getDepthTexNoTranslucent() : dh.getDepthTex();
    }

    public static boolean samplers(SamplerHolder holder) {
        if (!holder.hasSampler("skyweaveOpaqueDepth")) return false;
        if (((design.inherent.skyweavecompat.mixin.client.ConstructSamplerBudgetAccessor)holder)
                .skyweave$remainingUnits() <= 0) {
            com.mojang.logging.LogUtils.getLogger().warn("Construct occlusion unavailable: Iris has no spare texture unit for this program");
            return false;
        }
        return holder.addDynamicSampler(ConstructOcclusion::texture, "skyweaveOpaqueDepth");
    }
}
