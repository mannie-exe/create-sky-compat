package design.inherent.createskycompat.terrain;

/** Underground depth maps to the anchor body; above-surface heights retain block-scale relief. */
public record GeologicalFrame(int bottom, int topExclusive) {
    public static final int REFERENCE_BOTTOM = -64;
    public static final int REFERENCE_HEIGHT = 384;
    public static final int REFERENCE_SURFACE = 64;
    public GeologicalFrame {
        if (topExclusive <= bottom + 1) throw new IllegalArgumentException("Empty geological frame");
    }
    public boolean containsReference(int y) { return y >= REFERENCE_BOTTOM && y < 320; }
    public int toWorld(int referenceY) {
        if (referenceY >= REFERENCE_SURFACE) return surface() + referenceY - REFERENCE_SURFACE;
        return bottom + (int)Math.floorDiv((long)(referenceY - REFERENCE_BOTTOM) * (surface() - bottom),
                REFERENCE_SURFACE - REFERENCE_BOTTOM);
    }
    public int toReference(int worldY) {
        if (worldY >= surface()) return REFERENCE_SURFACE + worldY - surface();
        return REFERENCE_BOTTOM + (int)Math.floorDiv((long)(worldY - bottom) * (REFERENCE_SURFACE - REFERENCE_BOTTOM),
                surface() - bottom);
    }
    public int surface() { return topExclusive - 1; }
}
