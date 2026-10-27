package design.inherent.createskycompat.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import design.inherent.createskycompat.terrain.AncientCityBiomeScope;
import design.inherent.createskycompat.terrain.GeologicalGenerator;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.sathrek.sky_archipelago.worldgen.generator.core.SkyIslandChunkGenerator;
import org.sathrek.sky_archipelago.worldgen.generator.core.SkyIslandDeepDarkBiomeProjector;
import org.sathrek.sky_archipelago.worldgen.generator.structure.v2.common.PlacementRequest;
import org.sathrek.sky_archipelago.worldgen.generator.structure.v2.common.StructureGenerationAttemptService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Match preflight and native-layout biome queries for the registered ancient city only. */
@Mixin(value = StructureGenerationAttemptService.class, remap = false)
public abstract class AncientCityBiomeMixin {
    @WrapOperation(method = {"generate", "biomeValidityAt"}, at = @At(value = "INVOKE",
            target = "Lorg/sathrek/sky_archipelago/worldgen/generator/core/SkyIslandDeepDarkBiomeProjector;activateForStructureGeneration(Lorg/sathrek/sky_archipelago/worldgen/generator/core/SkyIslandChunkGenerator;Lnet/minecraft/world/level/levelgen/RandomState;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/world/level/levelgen/structure/Structure;)Lorg/sathrek/sky_archipelago/worldgen/generator/core/SkyIslandDeepDarkBiomeProjector$Scope;"))
    private static SkyIslandDeepDarkBiomeProjector.Scope cityScope(SkyIslandChunkGenerator generator,
            RandomState random, ChunkAccess chunk, Structure structure,
            Operation<SkyIslandDeepDarkBiomeProjector.Scope> original,
            @Local(argsOnly = true) PlacementRequest.StructureGenerationContext context) {
        var upstream = original.call(generator, random, chunk, structure);
        if (generator == null || random == null || structure == null || context == null
                || !((GeologicalGenerator)(Object)generator).createSky$isEnabled()
                || upstream == SkyIslandDeepDarkBiomeProjector.Scope.NOOP
                || !ResourceLocation.withDefaultNamespace("ancient_city").equals(
                        context.registryAccess().registryOrThrow(Registries.STRUCTURE).getKey(structure))) return upstream;
        return AncientCityBiomeScope.enter(generator, random, upstream);
    }
}
