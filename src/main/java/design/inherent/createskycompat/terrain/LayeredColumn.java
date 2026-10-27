package design.inherent.createskycompat.terrain;

import org.sathrek.sky_archipelago.worldgen.generator.field.TerrainColumn;
import java.util.List;

/** Exterior intervals survive internal carving; the native segments contain actual baseline solid. */
public interface LayeredColumn {
    List<TerrainColumn> createSky$exterior();
    void createSky$exterior(List<TerrainColumn> exterior);
    WaterColumn createSky$water();
    void createSky$water(WaterColumn water);
}
