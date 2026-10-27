package design.inherent.createskycompat.terrain;

/** Interior choices do not change descriptor geometry or the anchor's geological datum. */
public record InteriorProfile(double noiseChance, double hollowChance, int shellThickness,
        double noiseHorizontalScale, double noiseVerticalScale, double passageWidth,
        double chamberThreshold, double hollowRadius, double hollowHeight, double openingChance,
        boolean nativeCarvers) {
    public static final InteriorProfile INITIAL = new InteriorProfile(
            0.55, 0.18, 12, 64, 32, 0.075, 0.48, 0.58, 0.24, 0.35, true);
    public InteriorProfile {
        if (!finite(noiseChance, hollowChance, noiseHorizontalScale, noiseVerticalScale, passageWidth,
                chamberThreshold, hollowRadius, hollowHeight, openingChance)
                || noiseChance < 0 || hollowChance < 0 || noiseChance + hollowChance > 1
                || shellThickness < 4 || shellThickness > 48
                || noiseHorizontalScale < 16 || noiseHorizontalScale > 256
                || noiseVerticalScale < 8 || noiseVerticalScale > 128
                || passageWidth < 0.01 || passageWidth > 0.3
                || chamberThreshold < 0.2 || chamberThreshold > 1
                || hollowRadius < 0.2 || hollowRadius > 0.8
                || hollowHeight < 0.08 || hollowHeight > 0.35
                || openingChance < 0 || openingChance > 1) {
            throw new IllegalArgumentException("Invalid island interior profile");
        }
    }
    private static boolean finite(double... values) {
        for (double value : values) if (!Double.isFinite(value)) return false;
        return true;
    }
}
