package design.inherent.skyweavecompat.mixin;

import design.inherent.skyweavecompat.terrain.*;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.levelgen.RandomState;
import org.sathrek.sky_archipelago.worldgen.generator.core.SkyIslandChunkGenerator;
import org.sathrek.sky_archipelago.worldgen.generator.core.SkyIslandDeepDarkBiomeProjector;
import org.sathrek.sky_archipelago.worldgen.generator.field.model.ColumnProfile;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Project cave biomes within the native exterior, consistently for biome filling and structure checks. */
@Mixin(value = SkyIslandDeepDarkBiomeProjector.class, remap = false)
public abstract class CaveBiomeMixin {
    @Shadow @Final private static ThreadLocal<?> ACTIVE;
    @Shadow @Final private static ThreadLocal<Boolean> SAMPLING_PROXY;

    // Completed profiles can describe a roof after sidewall cleanup. Biome ownership
    // follows the uncarved native body in both stored BIOMES and structure checks.
    @WrapOperation(method = {
            "project(Lnet/minecraft/world/level/biome/BiomeSource;Lnet/minecraft/core/Holder;IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;",
            "project(Lnet/minecraft/world/level/biome/BiomeResolver;Lnet/minecraft/core/Holder;IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;"
    }, at = @At(value = "INVOKE", target =
            "Lorg/sathrek/sky_archipelago/worldgen/generator/core/SkyIslandChunkGenerator;sky_archipelago$columnProfileForBiomeProjection(Lnet/minecraft/world/level/levelgen/RandomState;III)Lorg/sathrek/sky_archipelago/worldgen/generator/field/model/ColumnProfile;"), require = 2)
    private static ColumnProfile exteriorBand(SkyIslandChunkGenerator generator, RandomState random,
            int x, int z, int maxHeight, Operation<ColumnProfile> original) {
        var geological = (GeologicalGenerator)(Object)generator;
        if (!geological.skyweave$isEnabled()) return original.call(generator,random,x,z,maxHeight);
        var field = (InteriorField)(Object)geological.skyweave$field(random);
        return field.skyweave$planningColumn(x,z,generator.resolvedSkyIslandSettings(),TerrainStage.EXTERIOR).profile();
    }

    @Inject(method = "project(Lnet/minecraft/world/level/biome/BiomeResolver;Lnet/minecraft/core/Holder;IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;",
            at = @At("RETURN"), cancellable = true)
    private static void relativeCaveBiome(BiomeResolver resolver, Holder<Biome> original, int qx, int qy, int qz,
            Climate.Sampler sampler, CallbackInfoReturnable<Holder<Biome>> result) {
        if (resolver == null || sampler == null || result.getReturnValue() == null || ACTIVE.get() == null
                || SAMPLING_PROXY.get()) return;
        var context = (DeepDarkContextAccessor)ACTIVE.get();
        var generator = context.skyweave$generator();
        if (!((GeologicalGenerator)(Object)generator).skyweave$isEnabled()) return;
        result.setReturnValue(CaveProjection.relative(generator,context.skyweave$random(),resolver,
                result.getReturnValue(),qx,qy,qz,sampler));
    }
}
