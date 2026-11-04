package design.inherent.skyweavecompat.mixin;

import design.inherent.skyweavecompat.terrain.GenerationProfile;
import design.inherent.skyweavecompat.terrain.ProfiledTerrain;
import design.inherent.skyweavecompat.terrain.InteriorResolver;
import design.inherent.skyweavecompat.terrain.IslandInteriors;
import design.inherent.skyweavecompat.terrain.TerrainStage;
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
    @Unique private GenerationProfile skyweave$profile;
    @Override public void skyweave$configure(GenerationProfile profile) { skyweave$profile = profile; }
    @Override public GenerationProfile skyweave$profile() { return skyweave$profile; }
    @Shadow @Final private IslandDensityEvaluator densityEvaluator;
    @Unique private IslandInteriors skyweave$interiors;
    @Override public void skyweave$interiors(IslandInteriors interiors) { skyweave$interiors = interiors; }

    @Inject(method = "resolveColumnDataFromContributors", at = @At("RETURN"), cancellable = true)
    private void interiors(int x, int z, SkyIslandSettings settings, int top, int bottom, long started,
            List<IslandField.ResolvedContributor> contributors, int checks, boolean snapshot,
            CallbackInfoReturnable<IslandField.ColumnData> result) {
        if (skyweave$interiors != null && TerrainStage.current() != TerrainStage.EXTERIOR)
            result.setReturnValue(skyweave$interiors.carve(x, z, settings, result.getReturnValue(), contributors, densityEvaluator));
    }

    @ModifyConstant(method = "resolveContributorsWithMetrics", constant = @Constant(intValue = 3), require = 4)
    private int searchRange(int original) { return skyweave$profile == null ? original : skyweave$profile.lookupCells(); }
}
