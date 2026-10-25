package design.inherent.createskycompat.climate;

/** Carries the owning surface pass into upstream selectors that have no world argument. */
public final class SurfaceClimateScope {
    private static final ThreadLocal<Boolean> ACTIVE = new ThreadLocal<>();
    private SurfaceClimateScope() {}
    public static boolean active() { return Boolean.TRUE.equals(ACTIVE.get()); }
    public static void run(boolean adapted, Runnable operation) {
        Boolean previous = ACTIVE.get();
        ACTIVE.set(adapted);
        try { operation.run(); }
        finally {
            if (previous == null) ACTIVE.remove(); else ACTIVE.set(previous);
        }
    }
}
