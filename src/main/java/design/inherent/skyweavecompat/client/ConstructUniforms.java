package design.inherent.skyweavecompat.client;

import com.mojang.blaze3d.shaders.ProgramManager;
import com.mojang.blaze3d.systems.RenderSystem;
import design.inherent.skyweavecompat.Feature;
import design.inherent.skyweavecompat.OptionalIntegrations;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.ranold.ssrd.Config;
import net.ranold.ssrd.client.DistanceLimits;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

/** Draw-time changes to one bound construct program, including nested and exceptional restoration. */
public final class ConstructUniforms {
    private static final ThreadLocal<Boolean> SINGLE_DRAW = ThreadLocal.withInitial(() -> false);
    private final int program, far, active, inverse, viewport, opaqueInverse;

    /** Carry classification through the immediate uploader's second shader apply. */
    public static void singleDraw(Runnable draw) {
        boolean previous = SINGLE_DRAW.get();
        SINGLE_DRAW.set(true);
        try { draw.run(); }
        finally {
            if (previous) SINGLE_DRAW.set(true);
            else SINGLE_DRAW.remove();
        }
    }

    public static boolean isSingleDraw() { return SINGLE_DRAW.get(); }
    public ConstructUniforms(int program, boolean depthSampler) {
        this.program = program;
        far = GL20.glGetUniformLocation(program, "far");
        active = depthSampler ? GL20.glGetUniformLocation(program, "skyweaveConstructOcclusion") : -1;
        inverse = GL20.glGetUniformLocation(program, "skyweaveConstructInverse");
        opaqueInverse = GL20.glGetUniformLocation(program, "skyweaveOpaqueInverse");
        viewport = GL20.glGetUniformLocation(program, "skyweaveConstructViewport");
    }

    public static void draw(ShaderInstance shader, Runnable draw) {
        var pipeline = Iris.getPipelineManager().getPipeline().orElse(null);
        if (!(shader instanceof ConstructProgram holder) || shader.PROJECTION_MATRIX == null
                || !(pipeline instanceof IrisRenderingPipeline iris) || !iris.shouldOverrideShaders()
                || IrisApi.getInstance().isRenderingShadowPass()
                || GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM) != shader.getId()) {
            draw.run(); return;
        }
        Matrix4f projection = new Matrix4f(shader.PROJECTION_MATRIX.getFloatBuffer());
        if (Math.abs(projection.m33()) >= 0.5f) { draw.run(); return; }
        holder.skyweave$constructUniforms().render(projection, draw);
    }

    private void render(Matrix4f projection, Runnable draw) {
        RenderSystem.assertOnRenderThread();
        boolean distance = far >= 0 && OptionalIntegrations.enabled(Feature.CONSTRUCT_DISTANCE);
        boolean occlusion = active >= 0 && OptionalIntegrations.enabled(Feature.CONSTRUCT_OCCLUSION)
                && ConstructOcclusion.ready();
        if (!distance && !occlusion) { draw.run(); return; }
        float oldFar = distance ? GL20.glGetUniformf(program, far) : 0;
        int oldActive = active >= 0 ? GL20.glGetUniformi(program, active) : 0;
        float[] oldInverse = occlusion ? new float[16] : null;
        float[] oldViewport = occlusion ? new float[4] : null;
        float[] oldOpaqueInverse = occlusion ? new float[16] : null;
        if (occlusion) {
            GL20.glGetUniformfv(program, inverse, oldInverse);
            GL20.glGetUniformfv(program, viewport, oldViewport);
            GL20.glGetUniformfv(program, opaqueInverse, oldOpaqueInverse);
        }
        try {
            if (distance && Float.isFinite(oldFar) && oldFar > 0) {
                var mc = Minecraft.getInstance();
                int maximum = DistanceLimits.query(mc.options.getEffectiveRenderDistance()).chunks();
                float blocks = Math.max(0, Math.min(Config.physicsRenderDistance, maximum)) * 16.0f;
                GL20.glUniform1f(far, Math.max(oldFar, blocks));
            }
            if (active >= 0) GL20.glUniform1i(active, occlusion ? 1 : 0);
            if (occlusion) {
                GL20.glUniformMatrix4fv(inverse, false, projection.invert().get(new float[16]));
                GL20.glUniformMatrix4fv(opaqueInverse, false, ConstructOcclusion.inverse().get(new float[16]));
                int[] rect = new int[4]; GL11.glGetIntegerv(GL11.GL_VIEWPORT, rect);
                GL20.glUniform4f(viewport, rect[0], rect[1], rect[2], rect[3]);
            }
            draw.run();
        } finally {
            // A nested renderer may have cleared or rebound its program. Preserve its final binding too.
            int bound = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
            if (bound != program) ProgramManager.glUseProgram(program);
            if (distance) GL20.glUniform1f(far, oldFar);
            if (active >= 0) GL20.glUniform1i(active, oldActive);
            if (occlusion) {
                GL20.glUniformMatrix4fv(inverse, false, oldInverse);
                GL20.glUniform4fv(viewport, oldViewport);
                GL20.glUniformMatrix4fv(opaqueInverse, false, oldOpaqueInverse);
            }
            if (bound != program) ProgramManager.glUseProgram(bound);
        }
    }
}
