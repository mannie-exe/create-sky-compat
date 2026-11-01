package design.inherent.createskycompat.recovery;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Finite nearest-first search. The caller decides whether a loaded candidate is safe. */
public final class RecoverySearch {
    public record Offset(int x, int y, int z) {
        int distanceSquared() { return x * x + y * y + z * z; }
    }
    public static final List<Offset> OFFSETS;
    static {
        var offsets = new ArrayList<Offset>();
        for (int x = -4; x <= 4; x++) for (int y = -3; y <= 3; y++) for (int z = -4; z <= 4; z++)
            offsets.add(new Offset(x, y, z));
        offsets.sort(Comparator.comparingInt(Offset::distanceSquared)
                .thenComparingInt(Offset::y).thenComparingInt(Offset::x).thenComparingInt(Offset::z));
        OFFSETS = List.copyOf(offsets);
    }
    private RecoverySearch() { }
}
