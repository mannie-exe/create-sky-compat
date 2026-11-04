package design.inherent.skyweavecompat.mixin;

import design.inherent.skyweavecompat.terrain.LayeredColumn;
import design.inherent.skyweavecompat.terrain.WaterColumn;
import org.sathrek.sky_archipelago.worldgen.generator.field.IslandField;
import org.sathrek.sky_archipelago.worldgen.generator.field.TerrainColumn;
import org.spongepowered.asm.mixin.*;
import java.util.List;

@Mixin(value = IslandField.ColumnData.class, remap = false)
public abstract class ColumnDataMixin implements LayeredColumn {
    @Unique private List<TerrainColumn> skyweave$exterior;
    @Unique private WaterColumn skyweave$water = WaterColumn.EMPTY;
    @Override public WaterColumn skyweave$water() { return skyweave$water; }
    @Override public void skyweave$water(WaterColumn water) { skyweave$water = water; }
    @Shadow public abstract List<TerrainColumn> segments();
    @Override public List<TerrainColumn> skyweave$exterior() {
        return skyweave$exterior == null ? segments() : skyweave$exterior;
    }
    @Override public void skyweave$exterior(List<TerrainColumn> exterior) {
        if (skyweave$exterior != null) throw new IllegalStateException("Column exterior already bound");
        skyweave$exterior = List.copyOf(exterior);
    }
}
