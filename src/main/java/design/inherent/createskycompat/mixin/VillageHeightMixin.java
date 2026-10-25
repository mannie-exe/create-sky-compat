package design.inherent.createskycompat.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import design.inherent.createskycompat.placement.VillageAnchors;
import design.inherent.createskycompat.placement.VillagePlan;
import design.inherent.createskycompat.terrain.GeologicalGenerator;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import org.sathrek.sky_archipelago.worldgen.generator.core.SkyIslandChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(JigsawStructure.class)
public abstract class VillageHeightMixin {
    @Unique private static final ResourceLocation CREATE_SKY_VILLAGE = ResourceLocation.parse("skyvillages:skyvillage");

    @WrapOperation(method = "findGenerationPoint", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/levelgen/heightproviders/HeightProvider;sample(Lnet/minecraft/util/RandomSource;Lnet/minecraft/world/level/levelgen/WorldGenerationContext;)I"))
    private int villageHeight(HeightProvider provider, RandomSource random, WorldGenerationContext height,
            Operation<Integer> original, @Local(argsOnly = true) Structure.GenerationContext context) {
        int sampled = original.call(provider, random, height);
        if (!(context.chunkGenerator() instanceof GeologicalGenerator generator) || !generator.createSky$isEnabled()
                || !CREATE_SKY_VILLAGE.equals(context.registryAccess().registryOrThrow(Registries.STRUCTURE).getKey((Structure)(Object)this))) {
            return sampled;
        }
        var sky = (SkyIslandChunkGenerator)context.chunkGenerator();
        var field = generator.createSky$field(context.randomState());
        int x = context.chunkPos().getMiddleBlockX(), z = context.chunkPos().getMiddleBlockZ();
        var owner = VillagePlan.owner(x, z, ((VillageAnchors)(Object)field).createSky$anchorsNear(x, z, sky.resolvedSkyIslandSettings()));
        // Preserve the native random spread, centered on the actual host rather than a world-wide band.
        return owner == null ? sampled : sampled + owner.surfaceY() - 192;
    }
}
