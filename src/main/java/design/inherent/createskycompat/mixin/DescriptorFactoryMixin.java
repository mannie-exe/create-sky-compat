package design.inherent.createskycompat.mixin;

import design.inherent.createskycompat.terrain.*;
import org.sathrek.sky_archipelago.config.SkyIslandSettings;
import org.sathrek.sky_archipelago.worldgen.generator.field.IslandField.ClusterDescriptor;
import org.sathrek.sky_archipelago.worldgen.generator.field.IslandField.IslandDescriptor;
import org.sathrek.sky_archipelago.worldgen.generator.field.internal.IslandDescriptorFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = IslandDescriptorFactory.class, remap = false)
public abstract class DescriptorFactoryMixin implements ProfiledTerrain {
    @Unique private GenerationProfile createSky$profile;
    @Override public void createSky$configure(GenerationProfile profile) { createSky$profile = profile; }
    @Override public GenerationProfile createSky$profile() { return createSky$profile; }

    @Inject(method = "createAnchorDescriptor", at = @At("RETURN"), cancellable = true)
    private void anchor(ClusterDescriptor cluster, SkyIslandSettings settings, CallbackInfoReturnable<IslandDescriptor> result) {
        if (createSky$profile != null) result.setReturnValue(IslandGeometry.island(result.getReturnValue(), cluster, createSky$profile));
    }
    @Inject(method = {"createSatelliteDescriptor", "createSpireDescriptor"}, at = @At("RETURN"), cancellable = true)
    private void companion(ClusterDescriptor cluster, SkyIslandSettings settings, int index,
            CallbackInfoReturnable<IslandDescriptor> result) {
        if (createSky$profile != null) result.setReturnValue(IslandGeometry.island(result.getReturnValue(), cluster, createSky$profile));
    }
}
