package design.inherent.skyweavecompat.mixin;

import design.inherent.skyweavecompat.terrain.*;
import org.sathrek.sky_archipelago.worldgen.generator.field.WaterArchetypeColumnProfile;
import org.sathrek.sky_archipelago.worldgen.generator.field.internal.IslandShapeSampler;
import org.sathrek.sky_archipelago.worldgen.generator.field.model.HorizontalSample;
import org.sathrek.sky_archipelago.worldgen.generator.field.model.IslandDescriptor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = IslandShapeSampler.class, remap = false)
public abstract class OceanShapeMixin implements ProfiledTerrain {
    @Unique private GenerationProfile skyweave$profile;
    @Override public void skyweave$configure(GenerationProfile profile) { skyweave$profile = profile; }
    @Override public GenerationProfile skyweave$profile() { return skyweave$profile; }

    @Inject(method = "sampleWaterArchetypeColumnProfile", at = @At("RETURN"), cancellable = true)
    private void waterEdge(IslandDescriptor island, HorizontalSample horizontal,
            CallbackInfoReturnable<WaterArchetypeColumnProfile> result) {
        if (skyweave$profile != null)
            result.setReturnValue(OceanIslands.adapt(island, horizontal, result.getReturnValue(), OceanConfiguration.profile()));
    }
}
