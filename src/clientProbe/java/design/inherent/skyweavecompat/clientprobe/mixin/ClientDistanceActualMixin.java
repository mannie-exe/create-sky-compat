package design.inherent.skyweavecompat.clientprobe.mixin;

import com.google.gson.JsonObject;
import design.inherent.skyweavecompat.clientprobe.ClientDistanceProbe;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "dev.ryanhcode.sable.sublevel.render.vanilla.VanillaChunkedSubLevelRenderData", remap = false)
abstract class ClientDistanceActualMixin {
    @Inject(method = "renderChunkedSubLevel", at = @At("HEAD"), require = 1)
    private void distance$actual(RenderType layer, ShaderInstance shader, Matrix4f view,
            double x, double y, double z, CallbackInfo ci) {
        if (!ClientDistanceProbe.captureActual || layer != RenderType.solid()
                || IrisApi.getInstance().isRenderingShadowPass()) return;
        ClientDistanceProbe.captureActual = false;
        var row = new JsonObject();
        for (String name : new String[]{"far", "skyweaveConstructOcclusion"}) {
            int location = GL20.glGetUniformLocation(shader.getId(), name);
            if (location >= 0) row.addProperty(name, name.equals("far")
                    ? GL20.glGetUniformf(shader.getId(), location) : GL20.glGetUniformi(shader.getId(), location));
        }
        int sampler = GL20.glGetUniformLocation(shader.getId(), "skyweaveOpaqueDepth");
        if (sampler >= 0) {
            int unit = GL20.glGetUniformi(shader.getId(), sampler);
            int old = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
            row.addProperty("depthUnit", unit);
            row.addProperty("depthTexture", GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D));
            row.addProperty("depthWidth", GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH));
            GL13.glActiveTexture(old);
        }
        if (ClientDistanceProbe.nestedFailure) {
            ClientDistanceProbe.nestedFailure = false;
            try {
                design.inherent.skyweavecompat.client.ConstructUniforms.draw(shader,
                        () -> { throw new design.inherent.skyweavecompat.clientprobe.ClientDistanceProbe.FixtureFailure(); });
            } catch (design.inherent.skyweavecompat.clientprobe.ClientDistanceProbe.FixtureFailure expected) {
                int far = GL20.glGetUniformLocation(shader.getId(), "far");
                int flag = GL20.glGetUniformLocation(shader.getId(), "skyweaveConstructOcclusion");
                row.addProperty("nestedFailureRestoredFar", GL20.glGetUniformf(shader.getId(), far));
                if (flag >= 0) row.addProperty("nestedFailureRestoredFlag", GL20.glGetUniformi(shader.getId(), flag));
            }
        }
        ClientDistanceProbe.actualDraw = row;
    }
}
