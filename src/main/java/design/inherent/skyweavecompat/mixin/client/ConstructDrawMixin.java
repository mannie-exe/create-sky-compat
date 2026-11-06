package design.inherent.skyweavecompat.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.MeshData;
import design.inherent.skyweavecompat.client.ConstructUniforms;
import dev.ryanhcode.sable.sublevel.render.vanilla.VanillaChunkedSubLevelRenderData;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Scope actual block draws, after SSRD's projection setup and after each shader apply. */
@Mixin(targets = "dev.ryanhcode.sable.sublevel.render.dispatcher.VanillaSubLevelRenderDispatcher", remap = false)
abstract class ConstructDrawMixin {
    @WrapOperation(method = "renderSectionLayer", at = @At(value = "INVOKE",
            target = "Ldev/ryanhcode/sable/sublevel/render/vanilla/VanillaChunkedSubLevelRenderData;renderChunkedSubLevel(Lnet/minecraft/client/renderer/RenderType;Lnet/minecraft/client/renderer/ShaderInstance;Lorg/joml/Matrix4f;DDD)V"), require = 1)
    private void skyweave$chunked(VanillaChunkedSubLevelRenderData data, RenderType layer, ShaderInstance shader,
            Matrix4f view, double x, double y, double z, Operation<Void> original) {
        ConstructUniforms.draw(shader, () -> original.call(data, layer, shader, view, x, y, z));
    }

    @WrapOperation(method = "renderAfterSections", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/RenderType;draw(Lcom/mojang/blaze3d/vertex/MeshData;)V"), require = 1)
    private void skyweave$single(RenderType layer, MeshData mesh, Operation<Void> original) {
        ConstructUniforms.singleDraw(() -> original.call(layer, mesh));
    }
}
