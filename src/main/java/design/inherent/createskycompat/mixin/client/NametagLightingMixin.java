package design.inherent.createskycompat.mixin.client;

import design.inherent.createskycompat.client.WorldTextLighting;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(EntityRenderer.class)
public abstract class NametagLightingMixin {
    @ModifyVariable(method = "renderNameTag(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/network/chat/Component;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IF)V",
            at = @At("HEAD"), argsOnly = true, require = 1, allow = 1)
    private MultiBufferSource createSky$nametagLighting(MultiBufferSource original) {
        return WorldTextLighting.wrap(original);
    }
}
