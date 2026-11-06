package design.inherent.skyweavecompat.clientprobe.mixin;

import design.inherent.skyweavecompat.clientprobe.ClientDistanceProbe;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** One requested observation from the actual opaque construct draw. */
@Mixin(targets = "dev.ryanhcode.sable.sublevel.render.dispatcher.VanillaSubLevelRenderDispatcher", remap = false, priority = 500)
abstract class ClientDistanceRenderMixin {
    @WrapMethod(method = "renderSectionLayer")
    private void distance$far(Iterable<ClientSubLevel> levels, RenderType layer,
            ShaderInstance shader, double x, double y, double z, Matrix4f modelView,
            Matrix4f projection, float partialTick, Operation<Void> original) {
        try { original.call(levels, layer, shader, x, y, z, modelView, projection, partialTick); }
        finally {
            if (ClientDistanceProbe.actualDraw != null && layer == RenderType.solid()
                    && !IrisApi.getInstance().isRenderingShadowPass()) {
                int far = GL20.glGetUniformLocation(shader.getId(), "far");
                int flag = GL20.glGetUniformLocation(shader.getId(), "skyweaveConstructOcclusion");
                if (far >= 0) ClientDistanceProbe.actualDraw.addProperty("farAfter", GL20.glGetUniformf(shader.getId(), far));
                if (flag >= 0) ClientDistanceProbe.actualDraw.addProperty("occlusionAfter", GL20.glGetUniformi(shader.getId(), flag));
            }
        }
    }

    @Inject(method = "renderSectionLayer", at = @At("HEAD"), require = 1)
    private void distance$draw(Iterable<ClientSubLevel> levels, RenderType layer,
            ShaderInstance shader, double x, double y, double z, Matrix4f modelView,
            Matrix4f projection, float partialTick, CallbackInfo ci) {
        if (!ClientDistanceProbe.captureNext || layer != RenderType.solid()
                || IrisApi.getInstance().isRenderingShadowPass()) return;
        ClientDistanceProbe.captureNext = false;
        var row = new JsonObject();
        row.addProperty("shader", shader.getName());
        row.addProperty("shaderClass", shader.getClass().getName());
        row.addProperty("program", shader.getId());
        row.addProperty("boundProgram", GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM));
        row.addProperty("depthFunction", GL11.glGetInteger(GL11.GL_DEPTH_FUNC));
        var proj = new JsonArray();
        for (float value : projection.get(new float[16])) proj.add(value);
        row.add("projection", proj);
        for (String name : new String[]{"far", "SableEnableNormalLighting", "gbufferProjection", "gbufferProjectionInverse", "iris_ProjMat", "iris_ProjMatInverse"}) {
            int location = GL20.glGetUniformLocation(shader.getId(), name);
            if (location < 0) continue;
            float[] values = new float[name.contains("Proj") ? 16 : 1];
            GL20.glGetUniformfv(shader.getId(), location, values);
            var array = new JsonArray();
            for (float value : values) array.add(value);
            row.add(name, array);
        }
        ClientDistanceProbe.draw = row;
    }
}
