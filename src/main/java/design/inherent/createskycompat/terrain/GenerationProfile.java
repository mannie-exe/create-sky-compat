package design.inherent.createskycompat.terrain;

/** Versioned terrain parameters. A changed layout requires a new generation identity. */
public record GenerationProfile(int spacing, int jitter, int centerMinY, int centerMaxY,
        int anchorMinRadius, int anchorMaxRadius, int anchorPlateau, int anchorCliff,
        int companionMinRadius, int companionMaxRadius, int companionMinOrbit, int companionMaxOrbit,
        int companionMinOffsetY, int companionMaxOffsetY, int minimumClusterGap,
        int floorClearance, int buildingHeadroom, double activation) {
    public static final String ID = "create_sky_compat:islands_v1";
    public static final int MIN_Y = -128;
    public static final int MAX_Y = 768;
    public static final GenerationProfile INITIAL = new GenerationProfile(
            4100, 90, 200, 490, 270, 370, 28, 290, 40, 110, 470, 680,
            -60, 50, 1500, 32, 200, 0.85);

    public GenerationProfile {
        if (spacing <= 0 || jitter < 0 || 2L * jitter >= spacing || centerMinY > centerMaxY
                || centerMinY < MIN_Y || centerMaxY >= MAX_Y
                || anchorMinRadius <= 0 || anchorMinRadius > anchorMaxRadius
                || anchorPlateau <= 0 || anchorCliff <= 0 || companionMinRadius <= 0
                || companionMinRadius > companionMaxRadius || companionMinOrbit < 0
                || companionMinOrbit > companionMaxOrbit || companionMinOffsetY > companionMaxOffsetY
                || minimumClusterGap < 0 || floorClearance < 0 || buildingHeadroom < 0
                || floorClearance > MAX_Y - MIN_Y || buildingHeadroom > MAX_Y - MIN_Y
                || companionMinOffsetY < MIN_Y - MAX_Y || companionMaxOffsetY > MAX_Y - MIN_Y
                || !Double.isFinite(activation) || activation <= 0 || activation > 1
                || spacing > 16384 || anchorMaxRadius > 500 || companionMaxRadius > 500
                || companionMaxOrbit > 8192 || anchorCliff > 600 || anchorPlateau > 128) {
            throw new IllegalArgumentException("Invalid or unreachable island profile");
        }
        int radius = clusterReach(anchorMaxRadius, companionMaxOrbit, companionMaxRadius);
        if ((long)spacing - 2L * jitter - 2L * radius < minimumClusterGap) {
            throw new IllegalArgumentException("Spacing/jitter cannot preserve minimumClusterGap around complete clusters");
        }
        if (safeMinimumY(centerMinY, anchorCliff, companionMaxOffsetY, floorClearance)
                > safeMaximumY(centerMaxY, anchorPlateau, companionMinOffsetY, buildingHeadroom)) {
            throw new IllegalArgumentException("No island placement fits the floor clearance and building headroom");
        }
    }

    // Sky Archipelago 1.3.6's native circular column envelope. Descriptor tails are checked separately.
    private static int clusterReach(int anchorRadius, int orbit, int companionRadius) {
        return Math.max(3 * anchorRadius + 96, orbit + 3 * Math.max(companionRadius, 28) + 97);
    }
    public int clusterReach() { return clusterReach(anchorMaxRadius, companionMaxOrbit, companionMaxRadius); }
    public int safeMinimumY() { return safeMinimumY(centerMinY, anchorCliff, companionMaxOffsetY, floorClearance); }
    public int safeMaximumY() { return safeMaximumY(centerMaxY, anchorPlateau, companionMinOffsetY, buildingHeadroom); }

    // Bounds include the native shoulder, capped hanging body, density fringe and surface relief.
    private static int safeMinimumY(int requested, int cliff, int companionOffset, int clearance) {
        int depth = Math.max(cliff + 48, 95 + 48 - companionOffset);
        return Math.max(requested, MIN_Y + clearance + depth);
    }
    private static int safeMaximumY(int requested, int plateau, int companionOffset, int headroom) {
        int rise = Math.max(plateau + 48, companionOffset + 14 + 48);
        return Math.min(requested, MAX_Y - 1 - headroom - rise);
    }
}
