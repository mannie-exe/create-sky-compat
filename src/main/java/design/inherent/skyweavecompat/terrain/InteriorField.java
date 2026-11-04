package design.inherent.skyweavecompat.terrain;

import net.minecraft.world.level.levelgen.RandomState;
import org.sathrek.sky_archipelago.config.SkyIslandSettings;
import org.sathrek.sky_archipelago.worldgen.generator.field.TerrainColumn;
import org.sathrek.sky_archipelago.worldgen.generator.field.IslandField;
import java.util.List;

public interface InteriorField {
    void skyweave$configureInteriors(RandomState random, InteriorProfile profile);
    List<TerrainColumn> skyweave$exteriorAt(int x, int z, SkyIslandSettings settings);
    IslandField.ColumnData skyweave$planningColumn(int x, int z, SkyIslandSettings settings, TerrainStage stage);
    WaterColumn skyweave$waterAt(int x, int z, SkyIslandSettings settings);
    IslandWaters skyweave$waters();
}
