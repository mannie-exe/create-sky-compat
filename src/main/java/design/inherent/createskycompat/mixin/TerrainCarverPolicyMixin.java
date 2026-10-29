package design.inherent.createskycompat.mixin;

import design.inherent.createskycompat.terrain.GeologicalGenerator;
import design.inherent.createskycompat.terrain.InteriorConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "org.sathrek.sky_archipelago.worldgen.generator.core.SkyIslandChunkGenerator", remap = false)
public abstract class TerrainCarverPolicyMixin {
    @Inject(method = "applyCarvers", at = @At("HEAD"), cancellable = true)
    private void createSky$carverPolicy(CallbackInfo callback) {
        if (((GeologicalGenerator)this).createSky$isEnabled() && !InteriorConfiguration.profile().nativeCarvers())
            callback.cancel();
    }
}
