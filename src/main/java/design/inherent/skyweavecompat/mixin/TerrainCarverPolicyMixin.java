package design.inherent.skyweavecompat.mixin;

import design.inherent.skyweavecompat.terrain.GeologicalGenerator;
import design.inherent.skyweavecompat.terrain.InteriorConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "org.sathrek.sky_archipelago.worldgen.generator.core.SkyIslandChunkGenerator", remap = false)
public abstract class TerrainCarverPolicyMixin {
    @Inject(method = "applyCarvers", at = @At("HEAD"), cancellable = true)
    private void skyweave$carverPolicy(CallbackInfo callback) {
        if (((GeologicalGenerator)this).skyweave$isEnabled() && !InteriorConfiguration.profile().nativeCarvers())
            callback.cancel();
    }
}
