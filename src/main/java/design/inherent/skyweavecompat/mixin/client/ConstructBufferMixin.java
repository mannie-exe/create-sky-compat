package design.inherent.skyweavecompat.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.VertexBuffer;
import design.inherent.skyweavecompat.client.ConstructUniforms;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** The single-block uploader applies its shader again before this actual draw. */
@Mixin(VertexBuffer.class)
abstract class ConstructBufferMixin {
    @WrapOperation(method = "_drawWithShader", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/VertexBuffer;draw()V"), require = 1)
    private void skyweave$singleDraw(VertexBuffer buffer, Operation<Void> original,
            Matrix4f view, Matrix4f projection, ShaderInstance shader) {
        if (ConstructUniforms.isSingleDraw())
            ConstructUniforms.draw(shader, () -> original.call(buffer));
        else original.call(buffer);
    }
}
