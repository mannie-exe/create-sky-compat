package design.inherent.skyweavecompat.terrain;

import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

/** Seed-bound material rules, configured before the owning field can be sampled. */
public interface VeinField {
    void skyweave$configureVeins(RandomState random, NoiseGeneratorSettings settings);
    IslandVeins skyweave$veins();
}
