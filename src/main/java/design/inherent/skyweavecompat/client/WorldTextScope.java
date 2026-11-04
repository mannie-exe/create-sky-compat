package design.inherent.skyweavecompat.client;

/** Shader selection is scoped at batch setup/draw, never at vertex emission. */
public final class WorldTextScope {
    private static final ThreadLocal<Boolean> ACTIVE = new ThreadLocal<>();
    private WorldTextScope() {}

    public static boolean active() { return Boolean.TRUE.equals(ACTIVE.get()); }

    public static void render(Runnable draw) {
        Boolean previous = ACTIVE.get();
        ACTIVE.set(true);
        try { draw.run(); }
        finally {
            if (previous == null) ACTIVE.remove();
            else ACTIVE.set(previous);
        }
    }
}
