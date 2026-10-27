package design.inherent.createskycompat.terrain;

import net.minecraft.world.level.levelgen.RandomState;
import org.sathrek.sky_archipelago.config.SkyIslandSettings;
import org.sathrek.sky_archipelago.worldgen.generator.field.TerrainColumn;
import org.sathrek.sky_archipelago.worldgen.generator.field.IslandField;
import java.util.List;

public interface InteriorField {
    void createSky$configureInteriors(RandomState random, InteriorProfile profile);
    List<TerrainColumn> createSky$exteriorAt(int x, int z, SkyIslandSettings settings);
    IslandField.ColumnData createSky$planningColumn(int x, int z, SkyIslandSettings settings, TerrainStage stage);
    WaterColumn createSky$waterAt(int x, int z, SkyIslandSettings settings);
    IslandWaters createSky$waters();
}
