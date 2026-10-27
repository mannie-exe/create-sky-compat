package design.inherent.createskycompat.terrain;

public record WaterProfile(double chance, int minRadius, int maxRadius, int minDepth, int maxDepth,
        double overflowChance, double outletChance, int maxOutlets, int outletWidth, int liningDepth) {
    public static final WaterProfile INITIAL = new WaterProfile(0.22, 20, 36, 6, 12, 0.25, 0.20, 1, 2, 3);
    public WaterProfile {
        if (!Double.isFinite(chance) || !Double.isFinite(overflowChance) || !Double.isFinite(outletChance)
                || chance < 0 || chance > 1 || overflowChance < 0 || outletChance < 0
                || overflowChance + outletChance > 1 || minRadius < 8 || maxRadius > 96 || maxRadius < minRadius
                || minDepth < 3 || maxDepth > 32 || maxDepth < minDepth || maxOutlets < 1 || maxOutlets > 3
                || outletWidth < 1 || outletWidth > 4 || liningDepth < 2 || liningDepth > 8)
            throw new IllegalArgumentException("Invalid local water profile");
    }
}
