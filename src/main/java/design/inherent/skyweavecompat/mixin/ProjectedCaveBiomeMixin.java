package design.inherent.skyweavecompat.mixin;

import design.inherent.skyweavecompat.terrain.*;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.levelgen.RandomState;
import org.sathrek.sky_archipelago.worldgen.generator.core.SkyIslandChunkGenerator;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Project cave ownership for physical queries and native locate candidate validation. */
@Mixin(value = SkyIslandChunkGenerator.class, remap = false)
public abstract class ProjectedCaveBiomeMixin {
    @Shadow @Final private Map<Climate.Sampler,RandomState> projectionRandomStates;
    @Shadow abstract BiomeSource rawBiomeSource();

    @Inject(method = "sky_archipelago$projectedBiome", at = @At("RETURN"), cancellable = true)
    private void caveBiome(int qx, int qy, int qz, Climate.Sampler sampler,
            CallbackInfoReturnable<Holder<Biome>> result) {
        if (!((GeologicalGenerator)(Object)this).skyweave$isEnabled() || DeepDarkProjectorAccess.skyweave$suppressed()) return;
        var random = projectionRandomStates.get(sampler);
        if (random == null) return;
        result.setReturnValue(CaveProjection.physical((SkyIslandChunkGenerator)(Object)this,random,rawBiomeSource(),
                result.getReturnValue(),qx,qy,qz,sampler));
    }
}
