package design.inherent.createskycompat.terrain;

import org.sathrek.sky_archipelago.worldgen.generator.field.IslandField.ClusterDescriptor;
import org.sathrek.sky_archipelago.worldgen.generator.field.IslandField.IslandDescriptor;
import org.sathrek.sky_archipelago.worldgen.generator.field.IslandField.IslandFamily;
import org.sathrek.sky_archipelago.worldgen.generator.field.internal.IslandNoise;

/** Retains upstream archetypes and shape noise while separating body and companion dimensions. */
public final class IslandGeometry {
    private IslandGeometry() {}

    public static ClusterDescriptor cluster(ClusterDescriptor source, GenerationProfile profile) {
        var noise = new IslandNoise(source.seed());
        int x = source.cellX() * profile.spacing() + profile.spacing() / 2
                + noise.sampleInt(source.seed(), 163, -profile.jitter(), profile.jitter());
        int z = source.cellZ() * profile.spacing() + profile.spacing() / 2
                + noise.sampleInt(source.seed(), 197, -profile.jitter(), profile.jitter());
        int y = noise.sampleInt(source.seed(), 223, profile.centerMinY(), profile.centerMaxY());
        return new ClusterDescriptor(source.cellX(), source.cellZ(), x, y, z,
                source.heightBand(), source.sizeBand(), source.tier(), source.baseRotation(),
                source.archetype(), source.satelliteCount(), source.spireCount(), source.seed());
    }

    public static IslandDescriptor island(IslandDescriptor source, ClusterDescriptor cluster,
            GenerationProfile profile) {
        var noise = new IslandNoise(source.seed());
        boolean anchor = source.family() == IslandFamily.ANCHOR_PLATEAU;
        boolean spire = source.family() == IslandFamily.SPIRE;
        int minRadius = anchor ? profile.anchorMinRadius() : spire ? 12 : profile.companionMinRadius();
        int maxRadius = anchor ? profile.anchorMaxRadius() : spire ? 28 : profile.companionMaxRadius();
        int rx = noise.sampleInt(source.seed(), 11, minRadius, maxRadius);
        int rz = noise.sampleInt(source.seed(), 13, minRadius, maxRadius);
        int x = cluster.centerX(), z = cluster.centerZ(), y = cluster.centerY();
        if (!anchor) {
            double angle = Math.atan2(source.centerZ() - cluster.centerZ(), source.centerX() - cluster.centerX());
            double orbit = noise.sampleInt(source.seed(), 7, profile.companionMinOrbit(), profile.companionMaxOrbit());
            x += (int)Math.round(Math.cos(angle) * orbit);
            z += (int)Math.round(Math.sin(angle) * orbit);
            // Independent of upstream height-band clamps, which describe the old world envelope.
            y += noise.sampleInt(source.seed(), 17, -30, 15);
        }
        int plateau = anchor ? profile.anchorPlateau() : spire ? 6 : 14;
        int cliff = anchor ? profile.anchorCliff() : spire ? 95 : noise.sampleInt(source.seed(), 31, 40, 90);
        return new IslandDescriptor(source.family(), x, y, z, rx, rz, Math.max(rx, rz),
                source.rotation(), source.archetype(), plateau, cliff, anchor ? 48 : 28,
                scale(source.hangOffsetX(), rx, source.radiusX()), scale(source.hangOffsetZ(), rz, source.radiusZ()),
                scale(source.tailRadiusX(), rx, source.radiusX()), scale(source.tailRadiusZ(), rz, source.radiusZ()),
                source.erosionStrength(), source.lobeCount(), source.peninsulaCount(), source.biteCount(), source.seed());
    }

    private static int scale(int value, int target, int original) {
        return (int)Math.round((double)value * target / Math.max(1, original));
    }
}
