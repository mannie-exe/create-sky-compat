package design.inherent.createskycompat.terrain;

import org.sathrek.sky_archipelago.worldgen.generator.field.IslandField.IslandFamily;
import org.sathrek.sky_archipelago.worldgen.generator.field.internal.IslandDensityEvaluator.PreparedDensity;
import org.sathrek.sky_archipelago.worldgen.generator.field.internal.IslandNoise;

/** Smooth lower support inside the existing body, with a bounded horizontal footprint. */
public final class IslandUnderside {
    private IslandUnderside() {}

    public record Bounds(boolean anchor, double bottom, double taper, double radialLimit) {
        public double density(PreparedDensity prepared, int y, double nativeDensity) {
            double result = nativeDensity;
            if (anchor) {
                // Remove only the pinned upstream discontinuity; retain native relief and erosion.
                if (y < prepared.underside() - 4) {
                    result += 0.55 + (prepared.underside() - 4 - y) * 0.055;
                }
                result = Math.min(result, (y - bottom) / taper);
            }
            return Math.min(result, radialLimit);
        }
    }

    public static Bounds prepare(GenerationProfile profile, PreparedDensity prepared, IslandNoise noise) {
        var island = prepared.descriptor();
        double dx = prepared.x() - island.centerX(), dz = prepared.z() - island.centerZ();
        double radialLimit = (GenerationProfile.supportRadius(island.maxRadius()) - Math.hypot(dx, dz)) / 32.0;
        boolean anchor = island.family() == IslandFamily.ANCHOR_PLATEAU;
        double bottom = 0;
        if (anchor) {
            double cos = Math.cos(island.rotation()), sin = Math.sin(island.rotation());
            double rx = (dx * cos + dz * sin) / island.radiusX();
            double rz = (-dx * sin + dz * cos) / island.radiusZ();
            double variation = Math.clamp(noise.fbm2D(island.seed() + 739,
                    prepared.x() * 0.018, prepared.z() * 0.018, 3, 0.5), -1.0, 1.0) * 8;
            bottom = island.centerY() - island.cliffDepth() - 16
                    + profile.undersideTaper() * (rx * rx + rz * rz) + variation;
        }
        return new Bounds(anchor, bottom, profile.undersideTaper(), radialLimit);
    }
}
