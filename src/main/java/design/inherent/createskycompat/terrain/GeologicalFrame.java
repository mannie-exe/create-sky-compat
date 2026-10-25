package design.inherent.createskycompat.terrain;

/** Island-wide affine map of the vanilla [-64, 320) reference envelope. */
public record GeologicalFrame(int bottom, int topExclusive) {
    public static final int REFERENCE_BOTTOM = -64;
    public static final int REFERENCE_HEIGHT = 384;
    public GeologicalFrame {
        if (topExclusive <= bottom) throw new IllegalArgumentException("Empty geological frame");
    }
    public boolean containsReference(int y) { return y >= REFERENCE_BOTTOM && y < 320; }
    public int toWorld(int referenceY) {
        return bottom + (int)Math.floorDiv((long)(referenceY - REFERENCE_BOTTOM) * (topExclusive - bottom), REFERENCE_HEIGHT);
    }
    public int toReference(int worldY) {
        return REFERENCE_BOTTOM + (int)Math.floorDiv((long)(worldY - bottom) * REFERENCE_HEIGHT, topExclusive - bottom);
    }
}
