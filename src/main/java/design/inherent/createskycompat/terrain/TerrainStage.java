package design.inherent.createskycompat.terrain;

import java.util.function.Supplier;

/** Bounded planning reads bypass later stages and the final-column cache. */
public enum TerrainStage {
    EXTERIOR, INTERIOR, COMPLETE;

    private static final ThreadLocal<TerrainStage> ACTIVE = ThreadLocal.withInitial(() -> COMPLETE);

    public static TerrainStage current() { return ACTIVE.get(); }

    public <T> T query(Supplier<T> query) {
        var previous = ACTIVE.get();
        ACTIVE.set(this);
        try { return query.get(); }
        finally {
            if (previous == COMPLETE) ACTIVE.remove(); else ACTIVE.set(previous);
        }
    }
}
