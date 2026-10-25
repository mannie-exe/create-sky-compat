package design.inherent.createskycompat.placement;

import org.sathrek.sky_archipelago.config.SkyIslandSettings;
import org.sathrek.sky_archipelago.worldgen.generator.field.IslandField;

public interface VillageStructureState {
    void createSky$configureVillages(IslandField field, SkyIslandSettings settings);
}
