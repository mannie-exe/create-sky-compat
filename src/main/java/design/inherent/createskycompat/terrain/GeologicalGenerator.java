package design.inherent.createskycompat.terrain;

import net.minecraft.world.level.levelgen.RandomState;
import org.sathrek.sky_archipelago.worldgen.generator.field.IslandField;

public interface GeologicalGenerator {
    boolean createSky$isEnabled();
    IslandField createSky$field(RandomState random);
}
