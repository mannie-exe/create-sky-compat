package design.inherent.skyweavecompat.terrain;

import org.sathrek.sky_archipelago.worldgen.generator.field.TerrainColumn;
import java.util.List;

/** Exterior intervals survive internal carving; the native segments contain actual baseline solid. */
public interface LayeredColumn {
    List<TerrainColumn> skyweave$exterior();
    void skyweave$exterior(List<TerrainColumn> exterior);
    WaterColumn skyweave$water();
    void skyweave$water(WaterColumn water);
}
