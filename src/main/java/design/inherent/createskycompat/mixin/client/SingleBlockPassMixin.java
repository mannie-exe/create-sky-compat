package design.inherent.createskycompat.mixin.client;

import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Flush single-block geometry while Iris still owns the corresponding terrain pass. */
@Mixin(targets = "dev.ryanhcode.sable.sublevel.render.dispatcher.VanillaSubLevelRenderDispatcher", remap = false)
abstract class SingleBlockPassMixin {
    @Shadow public abstract void renderAfterSections(Iterable<ClientSubLevel> levels,
            double x, double y, double z, Matrix4f modelView, Matrix4f projection, float partialTick);

    @Inject(method = "renderSectionLayer", at = @At("TAIL"), require = 1, allow = 1)
    private void createSky$flushCurrentPass(Iterable<ClientSubLevel> levels, RenderType layer,
            ShaderInstance shader, double x, double y, double z, Matrix4f modelView,
            Matrix4f projection, float partialTick, CallbackInfo ci) {
        if (IrisApi.getInstance().isShaderPackInUse())
            renderAfterSections(levels, x, y, z, modelView, projection, partialTick);
    }
}
