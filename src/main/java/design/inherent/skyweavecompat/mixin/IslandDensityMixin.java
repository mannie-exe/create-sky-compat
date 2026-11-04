package design.inherent.skyweavecompat.mixin;

import design.inherent.skyweavecompat.terrain.*;
import org.sathrek.sky_archipelago.worldgen.generator.field.internal.IslandDensityEvaluator;
import org.sathrek.sky_archipelago.worldgen.generator.field.internal.IslandDensityEvaluator.PreparedDensity;
import org.sathrek.sky_archipelago.worldgen.generator.field.internal.IslandNoise;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = IslandDensityEvaluator.class, remap = false)
public abstract class IslandDensityMixin implements ProfiledTerrain {
    @Shadow @Final private IslandNoise noise;
    @Unique private GenerationProfile skyweave$profile;
    @Override public void skyweave$configure(GenerationProfile profile) { skyweave$profile = profile; }
    @Override public GenerationProfile skyweave$profile() { return skyweave$profile; }

    @Inject(method = "prepareDensity", at = @At("RETURN"))
    private void prepare(org.sathrek.sky_archipelago.worldgen.generator.field.IslandField.IslandDescriptor descriptor,
            org.sathrek.sky_archipelago.worldgen.generator.field.IslandField.HorizontalSample horizontal,
            int x, int z, org.sathrek.sky_archipelago.config.SkyIslandSettings settings,
            org.sathrek.sky_archipelago.worldgen.generator.field.BiomeTerrainShaper.TerrainProfile terrain,
            CallbackInfoReturnable<PreparedDensity> result) {
        if (skyweave$profile != null) {
            ((PreparedIslandShape)(Object)result.getReturnValue()).skyweave$bounds(
                    IslandUnderside.prepare(skyweave$profile, result.getReturnValue(), noise));
        }
    }

    @Inject(method = "samplePreparedDensity", at = @At("RETURN"), cancellable = true)
    private void underside(PreparedDensity prepared, int y, CallbackInfoReturnable<Double> result) {
        var bounds = ((PreparedIslandShape)(Object)prepared).skyweave$bounds();
        if (bounds != null) result.setReturnValue(bounds.density(prepared, y, result.getReturnValue()));
    }
}
