package design.inherent.skyweavecompat.macaw;

/** Pixel bounds of the approved short-chain lantern, before blockstate rotation. */
public final class LanternLayout {
    public static final double SCALE = 8.0 / 11.0;
    public static final double TOP = 19.5 + .875 / Math.cos(Math.PI / 4) - 16;
    public static final double BOTTOM = TOP - 8;
    public static final double MIN_X = 8 - 3 * SCALE;
    public static final double MAX_X = 8 + 3 * SCALE;
    public static final double MIN_Z = 1 - 3 * SCALE;
    public static final double MAX_Z = 1 + 3 * SCALE;

    private LanternLayout() {}
}
