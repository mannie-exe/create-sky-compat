package design.inherent.skyweavecompat.placement;

import org.sathrek.sky_archipelago.config.SkyIslandSettings;
import org.sathrek.sky_archipelago.worldgen.generator.field.IslandField;

public interface VillageStructureState {
    void skyweave$configureVillages(IslandField field, SkyIslandSettings settings);
}
