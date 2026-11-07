package design.inherent.skyweavecompat.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import design.inherent.skyweavecompat.terrain.city.*;
import java.util.function.IntSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.sathrek.sky_archipelago.config.SkyIslandSettings;
import org.sathrek.sky_archipelago.worldgen.generator.field.IslandField;
import org.sathrek.sky_archipelago.worldgen.generator.field.TerrainColumn;
import org.sathrek.sky_archipelago.worldgen.generator.structure.v2.common.*;
import org.sathrek.sky_archipelago.worldgen.generator.structure.v2.underground.*;
import org.sathrek.sky_archipelago.worldgen.benchmark.StructureBenchmarkRecorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = UndergroundIslandFirstPlacementService.class, remap = false)
public abstract class AncientCityPlacementMixin {
    @WrapMethod(method = "place")
    private PlacementDecision scope(PlacementRequest request, UndergroundStrategySelector.Strategy strategy,
            Operation<PlacementDecision> original) {
        return CityPlacementScope.run(request, () -> original.call(request,strategy));
    }

    @WrapOperation(method = "place", at = @At(value = "INVOKE", target =
            "Lorg/sathrek/sky_archipelago/worldgen/generator/structure/v2/underground/UndergroundIslandFirstPlacementService;claimFor(Lnet/minecraft/resources/ResourceLocation;Lorg/sathrek/sky_archipelago/worldgen/generator/structure/v2/underground/UndergroundStrategySelector$Strategy;Lorg/sathrek/sky_archipelago/worldgen/generator/structure/v2/underground/UndergroundCanonicalSitePlanner$Site;)Lnet/minecraft/core/BlockPos;"))
    private BlockPos claim(ResourceLocation id, UndergroundStrategySelector.Strategy strategy,
            UndergroundCanonicalSitePlanner.Site site, Operation<BlockPos> original) {
        if (!CityPlacementScope.active()) return original.call(id,strategy,site);
        CityPlacementScope.select(site);
        var selected = CityPlacementScope.site();
        return selected == null ? original.call(id,strategy,site)
                : new BlockPos(site.generationChunk().getMiddleBlockX(),selected.floorY(),site.generationChunk().getMiddleBlockZ());
    }

    @WrapOperation(method = {"place", "claimFor"}, at = @At(value = "INVOKE", target =
            "Lorg/sathrek/sky_archipelago/worldgen/generator/structure/v2/underground/UndergroundIslandFirstPlacementService;usesDeepDarkSurfaceRoute(Lnet/minecraft/resources/ResourceLocation;Lorg/sathrek/sky_archipelago/worldgen/generator/structure/v2/underground/UndergroundStrategySelector$Strategy;)Z"), require = 2)
    private static boolean route(ResourceLocation id, UndergroundStrategySelector.Strategy strategy, Operation<Boolean> original) {
        return !CityPlacementScope.active() && original.call(id,strategy);
    }

    @WrapOperation(method = "place", at = @At(value = "INVOKE", target =
            "Lorg/sathrek/sky_archipelago/worldgen/generator/structure/v2/underground/UndergroundBiomeProbe;sampleY(Lnet/minecraft/world/level/levelgen/structure/Structure;Lorg/sathrek/sky_archipelago/worldgen/generator/field/TerrainColumn;Ljava/util/function/IntSupplier;)I"))
    private int biomeHeight(Structure structure, TerrainColumn column, IntSupplier fallback, Operation<Integer> original) {
        var selected = CityPlacementScope.site();
        return selected == null ? original.call(structure,column,fallback) : selected.floorY();
    }

    @WrapOperation(method = "place", at = @At(value = "INVOKE", target =
            "Lorg/sathrek/sky_archipelago/worldgen/generator/structure/v2/common/StructureGenerationAttemptService;biomeValidityAt(Lnet/minecraft/world/level/levelgen/structure/Structure;Lorg/sathrek/sky_archipelago/worldgen/generator/structure/v2/common/PlacementRequest$StructureGenerationContext;Lnet/minecraft/world/level/ChunkPos;I)Lorg/sathrek/sky_archipelago/worldgen/generator/structure/v2/common/StructureGenerationAttemptService$BiomeValidity;"))
    private StructureGenerationAttemptService.BiomeValidity biome(Structure structure,
            PlacementRequest.StructureGenerationContext context, ChunkPos chunk, int y,
            Operation<StructureGenerationAttemptService.BiomeValidity> original) {
        if (CityPlacementScope.active() && CityPlacementScope.site() == null)
            return new StructureGenerationAttemptService.BiomeValidity(true,false,"skyweave:no_city_interior");
        var result = original.call(structure,context,chunk,y);
        if (CityPlacementScope.active()) StructureBenchmarkRecorder.increment("skyweaveCityBiome." + result.biomeId());
        return result;
    }

    @WrapOperation(method = "place", at = @At(value = "INVOKE", target =
            "Lorg/sathrek/sky_archipelago/worldgen/generator/structure/v2/underground/UndergroundPieceFitEvaluator;findBestFit(Lnet/minecraft/world/level/levelgen/structure/StructureStart;Lorg/sathrek/sky_archipelago/worldgen/generator/structure/v2/underground/UndergroundStrategySelector$Strategy;Lorg/sathrek/sky_archipelago/worldgen/generator/structure/v2/underground/UndergroundCanonicalSitePlanner$Site;Lorg/sathrek/sky_archipelago/worldgen/generator/field/IslandField;Lorg/sathrek/sky_archipelago/config/SkyIslandSettings;Lorg/sathrek/sky_archipelago/worldgen/generator/structure/v2/underground/UndergroundSearchContext;)Lorg/sathrek/sky_archipelago/worldgen/generator/structure/v2/underground/UndergroundPieceFitEvaluator$Fit;"))
    private UndergroundPieceFitEvaluator.Fit fit(UndergroundPieceFitEvaluator evaluator, StructureStart start,
            UndergroundStrategySelector.Strategy strategy, UndergroundCanonicalSitePlanner.Site site,
            IslandField field, SkyIslandSettings settings, UndergroundSearchContext search,
            Operation<UndergroundPieceFitEvaluator.Fit> original) {
        if (!CityPlacementScope.active()) return original.call(evaluator,start,strategy,site,field,settings,search);
        var result = CityFit.fit(start,search);
        StructureBenchmarkRecorder.increment("skyweaveCityFit." + result.reason());
        if (result.accepted()) StructureBenchmarkRecorder.increment("skyweaveCityFit." +
                (CityPlacementScope.site().hollow() ? "hollow" : "normal"));
        StructureBenchmarkRecorder.add("skyweaveCityFit.samples", result.samples());
        return result;
    }

    @WrapOperation(method = "place", at = @At(value = "INVOKE", target =
            "Lorg/sathrek/sky_archipelago/worldgen/generator/structure/v2/underground/UndergroundPlacementCommitter;commitCanonical(Lorg/sathrek/sky_archipelago/worldgen/generator/structure/v2/common/PlacementRequest;Lnet/minecraft/world/level/levelgen/structure/StructureStart;Lnet/minecraft/world/level/ChunkPos;I)Lorg/sathrek/sky_archipelago/worldgen/generator/structure/v2/underground/UndergroundPlacementCommitter$CommitResult;"))
    private UndergroundPlacementCommitter.CommitResult commit(UndergroundPlacementCommitter committer,
            PlacementRequest request, StructureStart start, ChunkPos chunk, int delta,
            Operation<UndergroundPlacementCommitter.CommitResult> original) {
        var result = original.call(committer,request,start,chunk,delta);
        if (CityPlacementScope.active()) StructureBenchmarkRecorder.increment("skyweaveCityCommit." + result.reason());
        return result;
    }
}
