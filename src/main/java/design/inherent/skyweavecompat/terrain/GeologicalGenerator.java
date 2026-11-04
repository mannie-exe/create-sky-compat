package design.inherent.skyweavecompat.terrain;

import net.minecraft.world.level.levelgen.RandomState;
import org.sathrek.sky_archipelago.worldgen.generator.field.IslandField;

public interface GeologicalGenerator {
    boolean skyweave$isEnabled();
    IslandField skyweave$field(RandomState random);
}
