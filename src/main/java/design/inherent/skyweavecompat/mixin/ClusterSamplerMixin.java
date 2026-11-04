package design.inherent.skyweavecompat.mixin;

import design.inherent.skyweavecompat.terrain.*;
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
    @Unique private GenerationProfile skyweave$profile;
    @Override public void skyweave$configure(GenerationProfile profile) { skyweave$profile = profile; }
    @Override public GenerationProfile skyweave$profile() { return skyweave$profile; }

    @Inject(method = "sampleClusterDescriptor", at = @At("RETURN"), cancellable = true)
    private void cluster(int cellX, int cellZ, long seed, int spacing, SkyIslandSettings settings,
            int salt, CallbackInfoReturnable<ClusterDescriptor> result) {
        if (skyweave$profile != null) result.setReturnValue(IslandGeometry.cluster(result.getReturnValue(), skyweave$profile));
    }
}
