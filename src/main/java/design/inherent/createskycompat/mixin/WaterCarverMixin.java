package design.inherent.createskycompat.mixin;

import design.inherent.createskycompat.terrain.GeologicalCarving;
import design.inherent.createskycompat.terrain.InteriorField;
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
        var geology = ((GeologicalCarving)(Object)context).createSky$geology();
        if (geology == null) return;
        var water = ((InteriorField)(Object)geology.field()).createSky$waterAt(pos.getX(), pos.getZ(),
                geology.generator().resolvedSkyIslandSettings());
        if (water.contains(pos.getY())
                || water.regions().stream().anyMatch(region -> region.protects(pos.getX(), pos.getY(), pos.getZ())))
            result.setReturnValue(false);
    }
}
