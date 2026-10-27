package design.inherent.createskycompat.terrain;

import org.sathrek.sky_archipelago.worldgen.generator.field.TerrainColumn;
import java.util.List;

public record WaterColumn(List<TerrainColumn> sources, List<WaterRegion> regions) {
    public static final WaterColumn EMPTY = new WaterColumn(List.of(), List.of());
    public WaterColumn { sources = List.copyOf(sources); regions = List.copyOf(regions); }
    public boolean contains(int y) { return sources.stream().anyMatch(s -> s.contains(y)); }
    public int top(int fallback) { return sources.isEmpty() ? fallback : sources.getFirst().topY(); }
}
