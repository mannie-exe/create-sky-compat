package design.inherent.skyweavecompat.mixin;

import design.inherent.skyweavecompat.terrain.city.CityCavern;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.sathrek.sky_archipelago.worldgen.generator.core.SkyIslandChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SkyIslandChunkGenerator.class, remap = false)
public abstract class AncientCityTerrainMixin {
    @Inject(method = "adaptStructureTerrain", at = @At("RETURN"))
    private void clearCity(WorldGenLevel level, ChunkAccess chunk, StructureManager manager, CallbackInfo callback) {
        CityCavern.apply(level, chunk, manager, (SkyIslandChunkGenerator)(Object)this);
    }
}
