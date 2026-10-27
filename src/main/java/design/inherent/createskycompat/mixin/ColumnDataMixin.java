package design.inherent.createskycompat.mixin;

import design.inherent.createskycompat.terrain.LayeredColumn;
import design.inherent.createskycompat.terrain.WaterColumn;
import org.sathrek.sky_archipelago.worldgen.generator.field.IslandField;
import org.sathrek.sky_archipelago.worldgen.generator.field.TerrainColumn;
import org.spongepowered.asm.mixin.*;
import java.util.List;

@Mixin(value = IslandField.ColumnData.class, remap = false)
public abstract class ColumnDataMixin implements LayeredColumn {
    @Unique private List<TerrainColumn> createSky$exterior;
    @Unique private WaterColumn createSky$water = WaterColumn.EMPTY;
    @Override public WaterColumn createSky$water() { return createSky$water; }
    @Override public void createSky$water(WaterColumn water) { createSky$water = water; }
    @Shadow public abstract List<TerrainColumn> segments();
    @Override public List<TerrainColumn> createSky$exterior() {
        return createSky$exterior == null ? segments() : createSky$exterior;
    }
    @Override public void createSky$exterior(List<TerrainColumn> exterior) {
        if (createSky$exterior != null) throw new IllegalStateException("Column exterior already bound");
        createSky$exterior = List.copyOf(exterior);
    }
}
