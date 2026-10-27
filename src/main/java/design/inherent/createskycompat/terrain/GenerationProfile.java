package design.inherent.createskycompat.terrain;

/** Versioned terrain parameters. A changed layout requires a new generation identity. */
public record GenerationProfile(int spacing, int jitter, int centerMinY, int centerMaxY,
        int anchorMinRadius, int anchorMaxRadius, int anchorPlateau, int anchorMinHeight, int anchorMaxHeight, int undersideTaper,
        int companionMinRadius, int companionMaxRadius, int companionMinOrbit, int companionMaxOrbit,
        int companionMinOffsetY, int companionMaxOffsetY, int minimumClusterGap,
        int floorClearance, int buildingHeadroom, double activation) {
    public static final String ID = "create_sky_compat:islands_v1";
    public static final int MIN_Y = -128;
    public static final int MAX_Y = 768;
    public static final GenerationProfile INITIAL = new GenerationProfile(
            2400, 750, 200, 490, 270, 370, 28, 250, 400, 80, 40, 110, 470, 680,
            -60, 50, 350, 32, 200, 0.85);

    public GenerationProfile {
        if (spacing <= 0 || jitter < 0 || 2L * jitter >= spacing || centerMinY > centerMaxY
                || centerMinY < MIN_Y || centerMaxY >= MAX_Y
                || anchorMinRadius <= 0 || anchorMinRadius > anchorMaxRadius
                || anchorPlateau <= 0 || anchorMinHeight <= anchorPlateau + 64 || anchorMaxHeight < anchorMinHeight
                || undersideTaper < 16 || undersideTaper > anchorMinHeight / 2 || companionMinRadius <= 0
                || companionMinRadius > companionMaxRadius || companionMinOrbit < 0
                || companionMinOrbit > companionMaxOrbit || companionMinOffsetY > companionMaxOffsetY
                || minimumClusterGap < 0 || floorClearance < 0 || buildingHeadroom < 0
                || floorClearance > MAX_Y - MIN_Y || buildingHeadroom > MAX_Y - MIN_Y
                || companionMinOffsetY < MIN_Y - MAX_Y || companionMaxOffsetY > MAX_Y - MIN_Y
                || !Double.isFinite(activation) || activation <= 0 || activation > 1
                || spacing > 16384 || anchorMaxRadius > 500 || companionMaxRadius > 500
                || companionMaxOrbit > 8192 || anchorMaxHeight > 600 || anchorPlateau > 128) {
            throw new IllegalArgumentException("Invalid or unreachable island profile");
        }
        int radius = clusterReach(anchorMaxRadius, companionMaxOrbit, companionMaxRadius);
        if (Math.ceilDiv(2L * jitter + 2L * radius + minimumClusterGap, spacing) > 8) {
            throw new IllegalArgumentException("Profile requires more than eight neighboring cells for clearance");
        }
        if (safeMinimumY(centerMinY, anchorMaxHeight - anchorPlateau - 32, companionMaxOffsetY, floorClearance)
                > safeMaximumY(centerMaxY, anchorPlateau, companionMinOffsetY, buildingHeadroom)) {
            throw new IllegalArgumentException("No island placement fits the floor clearance and building headroom");
        }
    }

    // Explicit smooth density support; includes one block for rounded companion offsets.
    private static int clusterReach(int anchorRadius, int orbit, int companionRadius) {
        return Math.max(supportRadius(anchorRadius), orbit + supportRadius(Math.max(companionRadius, 28)) + 1);
    }
    public static int supportRadius(int radius) { return 2 * radius + 32; }
    public int cliffDepth(int targetHeight) { return targetHeight - anchorPlateau - 32; }
    public int safeMinimumY(int targetHeight) {
        return safeMinimumY(centerMinY, cliffDepth(targetHeight), companionMaxOffsetY, floorClearance);
    }
    public int clusterReach() { return clusterReach(anchorMaxRadius, companionMaxOrbit, companionMaxRadius); }
    /** A farther cell's centers cannot approach within two cluster reaches plus the gap. */
    public int conflictCells() { return (int)Math.ceilDiv(2L * jitter + 2L * clusterReach() + minimumClusterGap, spacing); }
    /** Point/area lookup, including the distance from a cell edge to its nominal center. */
    public int lookupCells() { return lookupCells(clusterReach()); }
    public int lookupCells(int reach) { return (int)Math.ceilDiv((long)jitter + reach + (spacing + 1L) / 2, spacing); }
    public int safeMinimumY() { return safeMinimumY(centerMinY, anchorMaxHeight - anchorPlateau - 32, companionMaxOffsetY, floorClearance); }
    public int safeMaximumY() { return safeMaximumY(centerMaxY, anchorPlateau, companionMinOffsetY, buildingHeadroom); }

    // Bounds include the native shoulder, capped hanging body, density fringe and surface relief.
    private static int safeMinimumY(int requested, int cliff, int companionOffset, int clearance) {
        int depth = Math.max(cliff + 24, 95 + 48 - companionOffset);
        return Math.max(requested, MIN_Y + clearance + depth);
    }
    private static int safeMaximumY(int requested, int plateau, int companionOffset, int headroom) {
        int rise = Math.max(plateau + 48, companionOffset + 14 + 48);
        return Math.min(requested, MAX_Y - 1 - headroom - rise);
    }
}
