package design.inherent.skyweavecompat.mixin;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import org.sathrek.sky_archipelago.worldgen.generator.core.SkyIslandDeepDarkBiomeProjector;
import org.sathrek.sky_archipelago.worldgen.generator.field.model.ColumnProfile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Reuse native projection policy without fabricating a structure-generation scope. */
@Mixin(value = SkyIslandDeepDarkBiomeProjector.class, remap = false)
public interface DeepDarkProjectorAccess {
    @Invoker("isProjectionSuppressed") static boolean skyweave$suppressed() { throw new AssertionError(); }
    @Invoker("isEligibleUndergroundBand") static boolean skyweave$eligible(ColumnProfile profile, int y) { throw new AssertionError(); }
    @Invoker("projectResolved") static Holder<Biome> skyweave$project(Holder<Biome> original, Holder<Biome> proxy,
            Holder<Biome> deepDark, ColumnProfile profile, int y) { throw new AssertionError(); }
}
