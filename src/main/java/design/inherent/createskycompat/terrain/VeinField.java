package design.inherent.createskycompat.terrain;

import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

/** Seed-bound material rules, configured before the owning field can be sampled. */
public interface VeinField {
    void createSky$configureVeins(RandomState random, NoiseGeneratorSettings settings);
    IslandVeins createSky$veins();
}
