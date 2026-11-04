package design.inherent.createskycompat.mixin.client;

import design.inherent.createskycompat.client.WorldTextLighting;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.DisplayRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(DisplayRenderer.TextDisplayRenderer.class)
public abstract class TextDisplayLightingMixin {
    @ModifyVariable(method = "renderInner(Lnet/minecraft/world/entity/Display$TextDisplay;Lnet/minecraft/world/entity/Display$TextDisplay$TextRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IF)V",
            at = @At("HEAD"), argsOnly = true, require = 1, allow = 1)
    private MultiBufferSource createSky$textDisplayLighting(MultiBufferSource original) {
        return WorldTextLighting.wrap(original);
    }
}
