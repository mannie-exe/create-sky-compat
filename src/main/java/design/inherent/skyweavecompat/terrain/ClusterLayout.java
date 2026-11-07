package design.inherent.skyweavecompat.terrain;

import java.util.List;

/** Local priority thinning of immutable candidates; never queries accepted neighbors. */
public final class ClusterLayout {
    public record Cell(int x, int z) {}
    public record Support(int x, int z, int halfWidth, int halfDepth) {
        public Support(int x, int z, int radius) { this(x, z, radius, radius); }
        public Support {
            if (halfWidth < 0 || halfDepth < 0) throw new IllegalArgumentException("Negative support extent");
        }
    }
    public record Candidate(Cell cell, long priority, List<Support> supports) {
        public Candidate { supports = List.copyOf(supports); }
    }
    @FunctionalInterface public interface Candidates { Candidate at(int cellX, int cellZ); }
    private ClusterLayout() {}

    public static boolean accepts(Candidate candidate, GenerationProfile profile, Candidates candidates) {
        int range = profile.conflictCells();
        for (int dx = -range; dx <= range; dx++) for (int dz = -range; dz <= range; dz++) {
            if (dx == 0 && dz == 0) continue;
            Candidate other = candidates.at(candidate.cell().x() + dx, candidate.cell().z() + dz);
            if (other != null && compare(other, candidate) < 0 && conflicts(candidate, other, profile.minimumClusterGap())) {
                return false;
            }
        }
        return true;
    }

    public static boolean conflicts(Candidate a, Candidate b, int gap) {
        for (Support left : a.supports()) for (Support right : b.supports()) {
            long dx = Math.abs((long)left.x() - right.x()) - left.halfWidth() - right.halfWidth();
            long dz = Math.abs((long)left.z() - right.z()) - left.halfDepth() - right.halfDepth();
            if (dx < 0 && dz < 0) return true;
            dx = Math.max(0, dx); dz = Math.max(0, dz);
            if (dx * dx + dz * dz < (long)gap * gap) return true;
        }
        return false;
    }

    private static int compare(Candidate a, Candidate b) {
        int order = Long.compareUnsigned(a.priority(), b.priority());
        if (order == 0) order = Integer.compare(a.cell().x(), b.cell().x());
        return order == 0 ? Integer.compare(a.cell().z(), b.cell().z()) : order;
    }
}
