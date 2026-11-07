package design.inherent.skyweavecompat.mixin;

import design.inherent.skyweavecompat.terrain.GeologicalCarving;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.levelgen.carver.*;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.function.Function;

@Mixin(WorldCarver.class)
public abstract class WaterCarverMixin {
    @Inject(method = "carveBlock", at = @At("HEAD"), cancellable = true)
    private void protectLining(CarvingContext context, CarverConfiguration config, ChunkAccess chunk,
            Function<BlockPos, Holder<Biome>> biomes, CarvingMask mask, BlockPos.MutableBlockPos pos,
            BlockPos.MutableBlockPos below, Aquifer aquifer, MutableBoolean surface,
            CallbackInfoReturnable<Boolean> result) {
        var geology = ((GeologicalCarving)(Object)context).skyweave$geology();
        if (geology == null) return;
        var field = geology.field();
        var settings = geology.generator().resolvedSkyIslandSettings();
        var water = field.sampleWaterArchetypeColumnProfile(pos.getX(), pos.getZ(), settings);
        var river = field.sampleHydrologyColumn(pos.getX(), pos.getZ(), settings);
        int y = pos.getY();
        if (water.containsWaterAt(y) || water.containsProtectedSealAt(y) || river.containsFluidAt(y)
                || river.affected() && y <= river.solidSurfaceY() && y >= river.solidSurfaceY() - 3)
            result.setReturnValue(false);
    }
}
