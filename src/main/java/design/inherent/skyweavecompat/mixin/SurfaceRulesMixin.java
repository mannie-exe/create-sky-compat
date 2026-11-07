package design.inherent.skyweavecompat.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import design.inherent.skyweavecompat.terrain.IslandSurfaceScope;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.sathrek.sky_archipelago.worldgen.generator.surface.IslandSurfaceRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = IslandSurfaceRules.class, remap = false)
public abstract class SurfaceRulesMixin {
    @WrapOperation(method = "paint", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/chunk/ChunkAccess;setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState surface(ChunkAccess chunk, BlockPos pos, BlockState state, boolean moved,
            Operation<BlockState> original) {
        var selected = IslandSurfaceScope.surface(chunk, pos, state);
        return selected == null ? chunk.getBlockState(pos) : original.call(chunk, pos, selected, moved);
    }
}
