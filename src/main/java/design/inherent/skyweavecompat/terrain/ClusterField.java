package design.inherent.skyweavecompat.terrain;

import java.util.List;
import org.sathrek.sky_archipelago.config.SkyIslandSettings;
import org.sathrek.sky_archipelago.worldgen.generator.field.IslandField.IslandDescriptor;

/** Accepted membership uses stable source cells, never a companion's displaced position. */
public interface ClusterField {
    List<IslandDescriptor> skyweave$members(int cellX, int cellZ, SkyIslandSettings settings);
    ClusterLayout.Cell skyweave$owner(IslandDescriptor descriptor, SkyIslandSettings settings);
}
