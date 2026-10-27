package design.inherent.createskycompat.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import design.inherent.createskycompat.terrain.ProfiledTerrain;
import design.inherent.createskycompat.terrain.BiomeMaterials;
import org.sathrek.sky_archipelago.config.SkyIslandSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.sathrek.sky_archipelago.worldgen.generator.field.IslandField;
import org.sathrek.sky_archipelago.worldgen.generator.surface.SkyIslandSurfacePass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = SkyIslandSurfacePass.class, remap = false)
public abstract class InteriorSurfaceMixin {
    @WrapOperation(method = "buildSurface", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/chunk/ChunkAccess;setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;"))
    private static BlockState preserveOpenings(ChunkAccess chunk, BlockPos pos, BlockState material, boolean moved,
            Operation<BlockState> original, @Local(argsOnly = true) IslandField field,
            @Local(argsOnly = true) SkyIslandSettings settings) {
        if (((ProfiledTerrain)(Object)field).createSky$profile() != null) {
            var current = chunk.getBlockState(pos);
            if (current.isAir() || !current.getFluidState().isEmpty()) return current;
            int surfaceY = field.sampleColumnProfile(pos.getX(), pos.getZ(), settings).topY();
            var biome = chunk.getNoiseBiome(pos.getX() >> 2, surfaceY >> 2, pos.getZ() >> 2);
            material = BiomeMaterials.surface(material, biome, surfaceY - pos.getY());
        }
        return original.call(chunk, pos, material, moved);
    }
}
