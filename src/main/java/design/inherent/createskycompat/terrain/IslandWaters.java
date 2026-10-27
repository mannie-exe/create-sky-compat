package design.inherent.createskycompat.terrain;

import org.sathrek.sky_archipelago.config.SkyIslandSettings;
import org.sathrek.sky_archipelago.worldgen.generator.field.IslandField;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Deterministic admission against earlier terrain. No chunks, fluid ticks or gameplay are read. */
public final class IslandWaters {
    private record Key(IslandField.IslandDescriptor island, SkyIslandSettings settings) {}
    private final ConcurrentHashMap<Key, Optional<WaterRegion>> cache = new ConcurrentHashMap<>();
    private final InteriorField field;
    private final WaterProfile profile;
    private final OceanProfile oceans;

    public IslandWaters(InteriorField field, WaterProfile profile, OceanProfile oceans) {
        this.field = field; this.profile = profile; this.oceans = oceans;
    }

    public WaterRegion region(IslandField.IslandDescriptor island, SkyIslandSettings settings) {
        var ocean = oceanKind(island);
        if (ocean == null && (island.family() != IslandField.IslandFamily.ANCHOR_PLATEAU
                || unit(island.seed() ^ 0x8a20c3d76b915ef4L) >= profile.chance())) return null;
        if (cache.size() > 512) cache.clear();
        return cache.computeIfAbsent(new Key(island, settings), key -> Optional.ofNullable(
                ocean == null ? plan(island, settings) : planOcean(island, settings, ocean))).orElse(null);
    }

    private WaterRegion.Kind oceanKind(IslandField.IslandDescriptor island) {
        if (!oceans.enabled()) return null;
        double selection = unit(island.seed() ^ 0x37fa60d3a82491b2L);
        if (island.family() == IslandField.IslandFamily.ANCHOR_PLATEAU) {
            if (selection < oceans.basinChance()) return WaterRegion.Kind.ENCLOSED;
            if (selection < oceans.basinChance() + oceans.coastalChance()) return WaterRegion.Kind.OVERFLOW;
        } else if (island.family() != IslandField.IslandFamily.SPIRE && island.maxRadius() <= oceans.perimeterMaxRadius()
                && selection < oceans.perimeterChance()) return WaterRegion.Kind.PERIMETER;
        return null;
    }

    private WaterRegion planOcean(IslandField.IslandDescriptor island, SkyIslandSettings settings, WaterRegion.Kind kind) {
        int depth = oceans.minDepth() + (int)(unit(island.seed() ^ 0x83ac2731L) * (oceans.maxDepth() - oceans.minDepth() + 1));
        boolean perimeter = kind == WaterRegion.Kind.PERIMETER;
        int radius = perimeter ? GenerationProfile.supportRadius(island.maxRadius())
                : oceans.minRadius() + (int)(unit(island.seed() ^ 0xb72cf913L) * (oceans.maxRadius() - oceans.minRadius() + 1));
        for (int attempt = 0; attempt < (perimeter ? 1 : 3); attempt++) {
            double angle = island.rotation() + attempt * Math.PI * 2 / 3;
            int x = island.centerX() + (perimeter ? 0 : (int)Math.round(Math.cos(angle) * island.radiusX() * 0.22));
            int z = island.centerZ() + (perimeter ? 0 : (int)Math.round(Math.sin(angle) * island.radiusZ() * 0.22));
            var center = field.createSky$planningColumn(x, z, settings, TerrainStage.EXTERIOR);
            if (center.segments().isEmpty() || center.dominantDescriptor() == null
                    || center.dominantDescriptor().seed() != island.seed()) continue;
            int top = center.segments().getFirst().topY();
            int level = top - 2;
            if (perimeter) {
                if (center.segments().getFirst().thickness() < depth + profile.liningDepth() + 4) continue;
                level = top - Math.max(4, depth / 2);
            } else {
                // A broader basin follows its lowest sampled rim instead of assuming a flat cap.
                for (int dx = -radius; dx <= radius; dx += 8) for (int dz = -radius; dz <= radius; dz += 8) {
                    if (dx * dx + dz * dz > radius * radius) continue;
                    var column = field.createSky$planningColumn(x + dx, z + dz, settings, TerrainStage.EXTERIOR);
                    if (column.segments().isEmpty()) { level = Integer.MIN_VALUE; continue; }
                    level = Math.min(level, column.segments().getFirst().topY() - 2);
                }
                if (level < top - oceans.maxDepth() - 4) continue;
            }
            var candidate = new WaterRegion(island.seed(), x, z, level, radius, depth, kind, angle,
                    GenerationProfile.supportRadius(island.maxRadius()) + radius, 1,
                    Math.max(4, profile.outletWidth()), profile.liningDepth(), true, oceans.beachWidth());
            // Perimeter caps deliberately end at their owning rock's edge; they have no retaining rim.
            if (perimeter || supports(candidate, settings, 8) && supports(candidate, settings, 1)) return candidate;
        }
        return null;
    }

    private WaterRegion plan(IslandField.IslandDescriptor island, SkyIslandSettings settings) {
        int radius = profile.minRadius() + (int)(unit(island.seed() ^ 0x772310a8L) * (profile.maxRadius() - profile.minRadius() + 1));
        int depth = profile.minDepth() + (int)(unit(island.seed() ^ 0x481561b9L) * (profile.maxDepth() - profile.minDepth() + 1));
        int outlets = 1 + (int)(unit(island.seed() ^ 0x4a6b38f1L) * profile.maxOutlets());
        double mode = unit(island.seed() ^ 0x17c894aaL);
        var kind = mode < profile.overflowChance() ? WaterRegion.Kind.OVERFLOW
                : mode < profile.overflowChance() + profile.outletChance() ? WaterRegion.Kind.OUTLET : WaterRegion.Kind.ENCLOSED;
        for (int attempt = 0; attempt < 3; attempt++) {
            double angle = island.rotation() + attempt * Math.PI * 2 / 3;
            int x = island.centerX() + (int)Math.round(Math.cos(angle) * island.radiusX() * 0.32);
            int z = island.centerZ() + (int)Math.round(Math.sin(angle) * island.radiusZ() * 0.32);
            var center = field.createSky$planningColumn(x, z, settings, TerrainStage.EXTERIOR);
            if (center.segments().isEmpty() || center.dominantDescriptor() == null
                    || center.dominantDescriptor().seed() != island.seed()) continue;
            int level = center.segments().getFirst().topY() - 2;
            var candidate = new WaterRegion(island.seed(), x, z, level, radius, depth, kind, angle,
                    GenerationProfile.supportRadius(island.maxRadius()) + radius, outlets, profile.outletWidth(), profile.liningDepth());
            // Reject obvious incomplete rims cheaply, then check every floor/rim column.
            if (!supports(candidate, settings, 8) || !supports(candidate, settings, 1)) continue;
            return candidate;
        }
        return null;
    }

    private boolean supports(WaterRegion region, SkyIslandSettings settings, int step) {
        int reach = region.radius() + 1;
        for (int dx = -reach; dx <= reach; dx += step) for (int dz = -reach; dz <= reach; dz += step) {
            if (dx * dx + dz * dz > reach * reach) continue;
            int x = region.x() + dx, z = region.z() + dz;
            int bottom = region.liningBottom(x, z);
            int top = region.level() + (region.basin(x, z) ? 0 : 1);
            var column = field.createSky$planningColumn(x, z, settings, TerrainStage.EXTERIOR);
            if (column.dominantDescriptor() == null || column.dominantDescriptor().seed() != region.owner()
                    || column.segments().stream().noneMatch(s -> s.bottomY() <= bottom && s.topY() >= top)) return false;
        }
        return true;
    }

    private static double unit(long value) {
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return ((value ^ (value >>> 31)) >>> 11) * 0x1.0p-53;
    }
}
