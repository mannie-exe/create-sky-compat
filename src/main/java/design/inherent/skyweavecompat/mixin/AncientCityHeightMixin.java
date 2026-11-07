package design.inherent.skyweavecompat.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import design.inherent.skyweavecompat.terrain.city.CityPlacementScope;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Native biome validation sees the proposed city height, never an unrelated lower-quarter sample. */
@Mixin(JigsawStructure.class)
public abstract class AncientCityHeightMixin {
    @WrapOperation(method = "findGenerationPoint", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/level/levelgen/heightproviders/HeightProvider;sample(Lnet/minecraft/util/RandomSource;Lnet/minecraft/world/level/levelgen/WorldGenerationContext;)I"))
    private int height(HeightProvider provider, RandomSource random, WorldGenerationContext context, Operation<Integer> original,
            @Local(argsOnly = true) Structure.GenerationContext generation) {
        int nativeY = original.call(provider,random,context);
        var site = CityPlacementScope.site();
        boolean applies = site != null && CityPlacementScope.isStructure(this)
                && generation.chunkGenerator() == CityPlacementScope.request().generationContext().generator()
                && generation.chunkPos().equals(site.nativeSite().generationChunk());
        if (CityPlacementScope.active()) org.sathrek.sky_archipelago.worldgen.benchmark.StructureBenchmarkRecorder
                .increment("skyweaveCityHeight." + (applies ? "adapted" : "unrelated"));
        return applies ? site.floorY() : nativeY;
    }
}
