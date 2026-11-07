package design.inherent.skyweavecompat.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import design.inherent.skyweavecompat.terrain.GeologicalGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import org.sathrek.sky_archipelago.worldgen.generator.core.SkyIslandChunkGenerator;
import org.sathrek.sky_archipelago.worldgen.generator.structure.v2.underground.UndergroundStrategySelector;
import org.sathrek.sky_archipelago.worldgen.structure.StructurePlacementCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Reach island-first planning before native city generation at vanilla Y=-27 can reject its biome. */
@Mixin(targets = "org.sathrek.sky_archipelago.worldgen.generator.structure.StructurePlacementLegacyCore", remap = false)
public abstract class AncientCityRoutingMixin {
    @WrapOperation(method = "tryGenerateValidatedStructureInner", at = @At(value = "INVOKE", target =
            "Lorg/sathrek/sky_archipelago/worldgen/generator/structure/StructurePlacementLegacyCore;shouldRouteUndergroundPreNative(ZLorg/sathrek/sky_archipelago/worldgen/structure/StructurePlacementCategory;Lorg/sathrek/sky_archipelago/worldgen/generator/structure/v2/underground/UndergroundStrategySelector$Strategy;)Z"))
    private static boolean route(boolean enabled, StructurePlacementCategory category,
            UndergroundStrategySelector.Strategy strategy, Operation<Boolean> original,
            @Local(argsOnly = true) SkyIslandChunkGenerator generator,
            @Local(argsOnly = true) StructureSet.StructureSelectionEntry entry) {
        return original.call(enabled,category,strategy) || enabled && category == StructurePlacementCategory.UNDERGROUND
                && strategy == UndergroundStrategySelector.Strategy.DYNAMIC_JIGSAW
                && ((GeologicalGenerator)(Object)generator).skyweave$isEnabled()
                && entry.structure().is(ResourceLocation.withDefaultNamespace("ancient_city"));
    }
}
