package design.inherent.createskycompat.mixin;

import net.minecraft.world.level.levelgen.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(OreVeinifier.class)
public interface OreVeinifierAccessor {
    @Invoker("create")
    static NoiseChunk.BlockStateFiller createSky$create(DensityFunction toggle, DensityFunction ridged,
            DensityFunction gap, PositionalRandomFactory random) {
        throw new AssertionError();
    }
}
