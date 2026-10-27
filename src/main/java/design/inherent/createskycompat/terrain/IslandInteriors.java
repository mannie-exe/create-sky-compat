package design.inherent.createskycompat.terrain;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.sathrek.sky_archipelago.worldgen.generator.field.IslandField;
import org.sathrek.sky_archipelago.worldgen.generator.field.TerrainColumn;
import org.sathrek.sky_archipelago.worldgen.generator.field.internal.IslandDensityEvaluator;
import org.sathrek.sky_archipelago.config.SkyIslandSettings;
import java.util.*;

/** Subtract each contributor's interior before union, within the admitted exterior intervals. */
public final class IslandInteriors {
    public enum Kind { BASELINE, NOISE, HOLLOW }
    private final InteriorProfile profile;
    private final NormalNoise passageA, passageB, chamber;
    private final IslandWaters waters;

    public IslandInteriors(RandomState random, InteriorProfile profile, IslandWaters waters) {
        this.profile = profile;
        this.waters = waters;
        var factory = random.getOrCreateRandomFactory(ResourceLocation.parse("create_sky_compat:interiors"));
        passageA = NormalNoise.create(factory.fromHashOf("passage_a"), new NormalNoise.NoiseParameters(-3, 1.0, 0.5));
        passageB = NormalNoise.create(factory.fromHashOf("passage_b"), new NormalNoise.NoiseParameters(-3, 1.0, 0.5));
        chamber = NormalNoise.create(factory.fromHashOf("chamber"), new NormalNoise.NoiseParameters(-3, 1.0, 0.5));
    }

    public static Kind kind(IslandField.IslandDescriptor island, InteriorProfile profile) {
        if (island.cliffDepth() < profile.shellThickness() * 4) return Kind.BASELINE;
        double selection = unit(island.seed() ^ 0x60bb123f7a84a1d5L);
        return selection < profile.hollowChance() ? Kind.HOLLOW
                : selection < profile.noiseChance() + profile.hollowChance() ? Kind.NOISE : Kind.BASELINE;
    }

    public IslandField.ColumnData carve(int x, int z, SkyIslandSettings settings, IslandField.ColumnData exterior,
            List<IslandField.ResolvedContributor> contributors, IslandDensityEvaluator density) {
        if (exterior.segments().isEmpty()) return exterior;
        var allowed = new BitSet(GenerationProfile.MAX_Y - GenerationProfile.MIN_Y);
        for (var segment : exterior.segments()) allowed.set(segment.bottomY() - GenerationProfile.MIN_Y,
                segment.topY() + 1 - GenerationProfile.MIN_Y);
        var retained = new BitSet(allowed.length());
        var waterBits = new BitSet(allowed.length());
        var regions = new ArrayList<WaterRegion>();
        for (var contributor : contributors) {
            var descriptor = contributor.descriptor();
            var kind = kind(descriptor, profile);
            boolean hollow = kind == Kind.HOLLOW;
            boolean active = kind != Kind.BASELINE;
            var water = TerrainStage.current() == TerrainStage.COMPLETE ? waters.region(descriptor, settings) : null;
            if (water != null && !water.affects(x, z)) water = null;
            if (water != null) regions.add(water);
            int top = Integer.MIN_VALUE;
            for (int y = contributor.maxY(); y >= contributor.minY() - 1; y--) {
                boolean solid = y >= contributor.minY() && density.samplePreparedDensity(contributor.preparedDensity(), y) > 0;
                if (solid) { if (top == Integer.MIN_VALUE) top = y; continue; }
                if (top == Integer.MIN_VALUE) continue;
                for (int sy = y + 1; sy <= top; sy++) {
                    int index = sy - GenerationProfile.MIN_Y;
                    if (!allowed.get(index)) continue;
                    boolean keep = !active || !removed(descriptor, x, sy, z, y + 1, top, hollow);
                    if (water != null) {
                        if (water.protects(x, sy, z)) keep = true;
                        if (water.cuts(x, sy, z)) keep = false;
                        if (water.source(x, sy, z)) waterBits.set(index);
                    }
                    if (keep) retained.set(index);
                }
                top = Integer.MIN_VALUE;
            }
        }
        if (retained.equals(allowed) && regions.isEmpty()) return exterior;
        waterBits.andNot(retained);
        // The native profile describes the exterior cap, never a newly exposed cavern floor.
        var result = new IslandField.ColumnData(intervals(retained), exterior.profile(), exterior.dominantDescriptor());
        ((LayeredColumn)(Object)result).createSky$exterior(exterior.segments());
        ((LayeredColumn)(Object)result).createSky$water(new WaterColumn(intervals(waterBits), regions));
        return result;
    }

    private static List<TerrainColumn> intervals(BitSet bits) {
        var segments = new ArrayList<TerrainColumn>();
        for (int top = bits.length() - 1; top >= 0;) {
            int bottom = bits.previousClearBit(top) + 1;
            segments.add(new TerrainColumn(bottom + GenerationProfile.MIN_Y, top + GenerationProfile.MIN_Y));
            top = bits.previousSetBit(bottom - 1);
        }
        return List.copyOf(segments);
    }

    private boolean removed(IslandField.IslandDescriptor island, int x, int y, int z, int bottom, int top, boolean hollow) {
        double dx = x - island.centerX(), dz = z - island.centerZ();
        double middle = island.centerY() - island.cliffDepth() * 0.35;
        double h = island.cliffDepth() * profile.hollowHeight();
        boolean openings = unit(island.seed() ^ 0x329df348abc51d27L) < profile.openingChance();
        if (openings && entrance(island, dx, y - middle, dz, h)) return true;
        if (y - bottom < profile.shellThickness() || top - y < profile.shellThickness()) return false;
        // Keep ragged outer shoulders solid, except for the explicitly selected entrance.
        double radial = dx * dx / (island.radiusX() * (double)island.radiusX())
                + dz * dz / (island.radiusZ() * (double)island.radiusZ());
        if (radial > 0.9 * 0.9) return false;
        double nx = x * 8 / profile.noiseHorizontalScale(), ny = y * 8 / profile.noiseVerticalScale();
        double nz = z * 8 / profile.noiseHorizontalScale();
        if (hollow) {
            double dy = (y - middle) / h;
            double shape = radial / (profile.hollowRadius() * profile.hollowRadius()) + dy * dy;
            if (shape >= 1 + chamber.getValue(nx * 0.6, ny * 0.6, nz * 0.6) * 0.18) return false;
            // Three irregular rock columns join roof and floor without dividing the entire hollow.
            for (int i = 0; i < 3; i++) {
                double angle = island.rotation() + i * Math.PI * 2 / 3;
                double distance = island.maxRadius() * (0.14 + unit(island.seed() + i * 437L) * 0.13);
                double px = Math.cos(angle) * distance + Math.sin(y * 0.035 + i) * 3;
                double pz = Math.sin(angle) * distance + Math.cos(y * 0.028 + i) * 3;
                double radius = 7 + unit(island.seed() ^ (i + 992L)) * 7;
                if ((dx - px) * (dx - px) + (dz - pz) * (dz - pz) < radius * radius) return false;
            }
            return true;
        }
        return (Math.abs(passageA.getValue(nx, ny, nz)) < profile.passageWidth()
                && Math.abs(passageB.getValue(nx, ny, nz)) < profile.passageWidth())
                || chamber.getValue(nx * 0.6, ny * 0.6, nz * 0.6) > profile.chamberThreshold();
    }

    private boolean entrance(IslandField.IslandDescriptor island, double dx, double dy, double dz, double h) {
        double c = Math.cos(island.rotation()), s = Math.sin(island.rotation());
        double along = dx * c + dz * s, across = dz * c - dx * s;
        double rise = Math.max(0, along) * 0.16;
        if (along > 0 && Math.abs(across) < 8 && Math.abs(dy - rise) < 8) return true;
        // A bounded skylight connects the hollow to the upper cap on selected islands.
        return dx * dx + dz * dz < 36 && dy > h * 0.5;
    }

    private static double unit(long value) {
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return ((value ^ (value >>> 31)) >>> 11) * 0x1.0p-53;
    }
}
