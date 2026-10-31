package design.inherent.createskycompat.mixin.client;

import design.inherent.createskycompat.lighting.ClientLightScope;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Both SDL client contributions are zero only inside a scoped server engine query. */
@Mixin(targets = {"fr0nick.sabledynlights.virtual.VirtualLightManager",
        "fr0nick.sabledynlights.lamb.LambDynLightsDelegate"}, remap = false)
public abstract class ScopedLightQueryMixin {
    @Inject(method = {"getVirtualEmission(Lnet/minecraft/core/BlockPos;)I",
            "getDynamicLightLevel(Lnet/minecraft/core/BlockPos;)I"},
            at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private static void createSky$excludeClientLight(BlockPos position, CallbackInfoReturnable<Integer> result) {
        if (ClientLightScope.serverQuery()) result.setReturnValue(0);
    }
}
