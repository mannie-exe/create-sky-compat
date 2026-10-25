package design.inherent.createskycompat.terrain;

/** Versioned terrain parameters. A changed layout requires a new generation identity. */
public record GenerationProfile(int spacing, int jitter, int centerMinY, int centerMaxY,
        int anchorMinRadius, int anchorMaxRadius, int anchorPlateau, int anchorCliff,
        int companionMinRadius, int companionMaxRadius, int companionMinOrbit, int companionMaxOrbit) {
    public static final String ID = "create_sky_compat:islands_v1";
    public static final int MIN_Y = -128;
    public static final int MAX_Y = 768;
    public static final GenerationProfile INITIAL = new GenerationProfile(
            1600, 180, 470, 490, 270, 370, 28, 290, 40, 110, 470, 680);

    public GenerationProfile {
        if (spacing <= 0 || jitter < 0 || 2 * jitter >= spacing || centerMinY > centerMaxY
                || anchorMinRadius <= 0 || anchorMinRadius > anchorMaxRadius
                || anchorPlateau <= 0 || anchorCliff <= 0 || companionMinRadius <= 0
                || companionMinRadius > companionMaxRadius || companionMinOrbit < 0
                || companionMinOrbit > companionMaxOrbit
                || companionMaxOrbit + 3 * companionMaxRadius >= spacing * 3) {
            throw new IllegalArgumentException("Invalid or unreachable island profile");
        }
    }
}
