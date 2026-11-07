package design.inherent.skyweavecompat.terrain.city;

import java.util.List;

/** Finite piece influence and retained rock, independent of world/chunk APIs. */
public final class CityClearance {
    public static final int RADIUS = 12;
    // Vanilla overworld density is squeezed before the structure contribution.
    // Its upper bound preserves the smallest clearance the native kernel provides.
    public static final double BASE_DENSITY = 11.0 / 24.0;
    public record Piece(StructureBurial.Box box, int groundY) {}
    @FunctionalInterface public interface Body { Range column(int x, int z); }
    public record Range(int bottom, int top) {
        public static final Range EMPTY = new Range(1, 0);
        public boolean contains(int y) { return y >= bottom && y <= top; }
        public Range intersect(Range other) { return new Range(Math.max(bottom, other.bottom), Math.min(top, other.top)); }
    }
    private CityClearance() {}

    public static Range influence(List<Piece> pieces, int x, int z) {
        int bottom = Integer.MAX_VALUE, top = Integer.MIN_VALUE;
        for (var piece : pieces) {
            var box = piece.box();
            if (x < box.minX() - RADIUS || x >= box.maxX() + RADIUS
                    || z < box.minZ() - RADIUS || z >= box.maxZ() + RADIUS) continue;
            bottom = Math.min(bottom, piece.groundY() + 1);
            top = Math.max(top, box.maxY() - 1 + RADIUS - 1);
        }
        return new Range(bottom, top);
    }

    /** Erode the exterior by a complete block-radius neighborhood, including narrow notches. */
    public static Range retainShell(int x, int z, int thickness, Body body) {
        int bottom = Integer.MIN_VALUE, top = Integer.MAX_VALUE;
        for (int dx = -thickness; dx <= thickness; dx++) for (int dz = -thickness; dz <= thickness; dz++) {
            var column = body.column(x + dx, z + dz);
            bottom = Math.max(bottom, column.bottom() + thickness);
            top = Math.min(top, column.top() - thickness);
            if (bottom > top) return Range.EMPTY;
        }
        return new Range(bottom, top);
    }

    public static boolean protectsFloor(List<Piece> pieces, int x, int y, int z) {
        for (var piece : pieces) {
            var box = piece.box();
            if (x >= box.minX() && x < box.maxX() && z >= box.minZ() && z < box.maxZ()
                    && y >= piece.groundY() - 4 && y <= piece.groundY()) return true;
        }
        return false;
    }

    public static boolean clears(double contribution) {
        return Double.isFinite(contribution) && contribution + BASE_DENSITY < 0;
    }

    /** Burial elsewhere cannot compensate for a core without cover or footing. */
    public static boolean coreFits(Range body, int floor, int top, int shell, boolean supported) {
        return supported && body.contains(floor - shell) && body.contains(top + shell);
    }
}
