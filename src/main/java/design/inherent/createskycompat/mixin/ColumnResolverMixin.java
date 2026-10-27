package design.inherent.createskycompat.mixin;

import design.inherent.createskycompat.terrain.GenerationProfile;
import design.inherent.createskycompat.terrain.ProfiledTerrain;
import design.inherent.createskycompat.terrain.InteriorResolver;
import design.inherent.createskycompat.terrain.IslandInteriors;
import design.inherent.createskycompat.terrain.TerrainStage;
import org.sathrek.sky_archipelago.config.SkyIslandSettings;
import org.sathrek.sky_archipelago.worldgen.generator.field.IslandField;
import org.sathrek.sky_archipelago.worldgen.generator.field.internal.IslandDensityEvaluator;
import org.sathrek.sky_archipelago.worldgen.generator.field.internal.IslandColumnResolver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(value = IslandColumnResolver.class, remap = false)
public abstract class ColumnResolverMixin implements ProfiledTerrain, InteriorResolver {
    @Unique private GenerationProfile createSky$profile;
    @Override public void createSky$configure(GenerationProfile profile) { createSky$profile = profile; }
    @Override public GenerationProfile createSky$profile() { return createSky$profile; }
    @Shadow @Final private IslandDensityEvaluator densityEvaluator;
    @Unique private IslandInteriors createSky$interiors;
    @Override public void createSky$interiors(IslandInteriors interiors) { createSky$interiors = interiors; }

    @Inject(method = "resolveColumnDataFromContributors", at = @At("RETURN"), cancellable = true)
    private void interiors(int x, int z, SkyIslandSettings settings, int top, int bottom, long started,
            List<IslandField.ResolvedContributor> contributors, int checks, boolean snapshot,
            CallbackInfoReturnable<IslandField.ColumnData> result) {
        if (createSky$interiors != null && TerrainStage.current() != TerrainStage.EXTERIOR)
            result.setReturnValue(createSky$interiors.carve(x, z, settings, result.getReturnValue(), contributors, densityEvaluator));
    }

    @ModifyConstant(method = "resolveContributorsWithMetrics", constant = @Constant(intValue = 3), require = 4)
    private int searchRange(int original) { return createSky$profile == null ? original : createSky$profile.lookupCells(); }
}
