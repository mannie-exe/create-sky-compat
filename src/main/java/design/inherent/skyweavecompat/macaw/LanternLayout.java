package design.inherent.skyweavecompat.macaw;

/** Pixel bounds of approved bridge lantern models, before blockstate rotation. */
public final class LanternLayout {
    public static final double FLAT_MIN_X = 5.5, FLAT_MAX_X = 10.5;
    public static final double FLAT_MIN_Z = -1, FLAT_MAX_Z = 4;
    public static final double FLAT_BOTTOM = -1, FLAT_TOP = 7;
    public static final double SCALE = 8.0 / 11.0;
    public static final double TOP = 19.5 + .875 / Math.cos(Math.PI / 4) - 16;
    public static final double BOTTOM = TOP - 8;
    public static final double MIN_X = 8 - 3 * SCALE;
    public static final double MAX_X = 8 + 3 * SCALE;
    public static final double MIN_Z = 1 - 3 * SCALE;
    public static final double MAX_Z = 1 + 3 * SCALE;

    private LanternLayout() {}
}
