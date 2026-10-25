package design.inherent.createskycompat.mixin;

import design.inherent.createskycompat.terrain.*;
import org.sathrek.sky_archipelago.config.SkyIslandSettings;
import org.sathrek.sky_archipelago.worldgen.generator.field.IslandField.ClusterDescriptor;
import org.sathrek.sky_archipelago.worldgen.generator.field.internal.IslandClusterSampler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = IslandClusterSampler.class, remap = false)
public abstract class ClusterSamplerMixin implements ProfiledTerrain {
    @Unique private GenerationProfile createSky$profile;
    @Override public void createSky$configure(GenerationProfile profile) { createSky$profile = profile; }
    @Override public GenerationProfile createSky$profile() { return createSky$profile; }

    @Inject(method = "sampleClusterDescriptor", at = @At("RETURN"), cancellable = true)
    private void cluster(int cellX, int cellZ, long seed, int spacing, SkyIslandSettings settings,
            int salt, CallbackInfoReturnable<ClusterDescriptor> result) {
        if (createSky$profile != null) result.setReturnValue(IslandGeometry.cluster(result.getReturnValue(), createSky$profile));
    }
}
