package design.inherent.createskycompat.mixin;

import design.inherent.createskycompat.placement.VillageStructureState;
import design.inherent.createskycompat.terrain.GeologicalGenerator;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import org.sathrek.sky_archipelago.worldgen.generator.core.SkyIslandChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkGenerator.class)
public abstract class GeneratorStructureStateMixin {
    @Inject(method = "createState", at = @At("RETURN"))
    private void villages(HolderLookup<StructureSet> sets, RandomState random, long seed,
            CallbackInfoReturnable<ChunkGeneratorStructureState> result) {
        if ((Object)this instanceof SkyIslandChunkGenerator sky && ((GeologicalGenerator)(Object)sky).createSky$isEnabled()) {
            var field = ((GeologicalGenerator)(Object)sky).createSky$field(random);
            ((VillageStructureState)result.getReturnValue()).createSky$configureVillages(field, sky.resolvedSkyIslandSettings());
        }
    }
}
