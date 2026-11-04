package design.inherent.skyweavecompat.mixin;

import net.minecraft.world.level.levelgen.RandomState;
import org.sathrek.sky_archipelago.worldgen.generator.core.SkyIslandChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "org.sathrek.sky_archipelago.worldgen.generator.core.SkyIslandDeepDarkBiomeProjector$Context", remap = false)
public interface DeepDarkContextAccessor {
    @Accessor("generator") SkyIslandChunkGenerator skyweave$generator();
    @Accessor("randomState") RandomState skyweave$random();
}
