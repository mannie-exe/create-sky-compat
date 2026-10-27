package design.inherent.createskycompat.terrain;

/** Experimental local habitats. Probabilities apply before terrain admission. */
public record OceanProfile(boolean enabled, double basinChance, double coastalChance, double perimeterChance,
        int minRadius, int maxRadius, int minDepth, int maxDepth, int perimeterMaxRadius, int beachWidth) {
    public static final OceanProfile INITIAL = new OceanProfile(false, 0.04, 0.04, 0.01, 40, 72, 10, 18, 64, 6);
    public OceanProfile {
        if (!Double.isFinite(basinChance) || !Double.isFinite(coastalChance) || !Double.isFinite(perimeterChance)
                || basinChance < 0 || coastalChance < 0 || basinChance + coastalChance > 1
                || perimeterChance < 0 || perimeterChance > 1 || minRadius < 16 || maxRadius > 96 || minRadius > maxRadius
                || minDepth < 4 || maxDepth > 32 || minDepth > maxDepth || perimeterMaxRadius < 32 || perimeterMaxRadius > 96
                || beachWidth < 2 || beachWidth > 16) throw new IllegalArgumentException("Invalid ocean habitat profile");
    }
}
