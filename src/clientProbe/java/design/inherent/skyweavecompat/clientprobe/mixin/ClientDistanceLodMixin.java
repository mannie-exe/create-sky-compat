package design.inherent.skyweavecompat.clientprobe.mixin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import design.inherent.skyweavecompat.clientprobe.ClientDistanceProbe;
import net.irisshaders.iris.compat.dh.DHCompat;
import net.irisshaders.iris.api.v0.IrisApi;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Compare the matrix used to draw LODs with the public matrix used to reconstruct them. */
@Mixin(targets = "net.irisshaders.iris.compat.dh.IrisLodRenderProgram", remap = false)
abstract class ClientDistanceLodMixin {
    @Inject(method = "fillUniformData", at = @At("HEAD"), require = 1)
    private void distance$lod(Matrix4fc projection, Matrix4fc view, int y, float tick, CallbackInfo ci) {
        if (!ClientDistanceProbe.captureLodNext || IrisApi.getInstance().isRenderingShadowPass()) return;
        ClientDistanceProbe.captureLodNext = false;
        var row = new JsonObject();
        var used = new JsonArray();
        for (float value : projection.get(new float[16])) used.add(value);
        row.add("projection", used);
        var expected = new JsonArray();
        for (float value : DHCompat.getProjection().get(new float[16])) expected.add(value);
        row.add("reconstruction", expected);
        row.addProperty("near", DHCompat.getNearPlane());
        row.addProperty("far", DHCompat.getFarPlane());
        ClientDistanceProbe.lodDraw = row;
    }
}
