package design.inherent.createskycompat.terrain;

/** Finite geometry only. Sources flow through openings during Minecraft's normal fluid ticks. */
public record WaterRegion(long owner, int x, int z, int level, int radius, int depth, Kind kind,
        double heading, int reach, int outlets, int width, int liningDepth, boolean ocean, int beachWidth) {
    public enum Kind { ENCLOSED, OVERFLOW, OUTLET, PERIMETER }

    public WaterRegion(long owner, int x, int z, int level, int radius, int depth, Kind kind,
            double heading, int reach, int outlets, int width, int liningDepth) {
        this(owner, x, z, level, radius, depth, kind, heading, reach, outlets, width, liningDepth, false, 0);
    }

    public boolean basin(int px, int pz) {
        long dx = (long)px - x, dz = (long)pz - z;
        return dx * dx + dz * dz < (long)radius * radius;
    }
    public int bed(int px, int pz) {
        if (kind == Kind.PERIMETER) return level - depth;
        double dx = (double)px - x, dz = (double)pz - z;
        double fraction = Math.max(0, 1 - (dx * dx + dz * dz) / (radius * (double)radius));
        return level - Math.max(1, (int)Math.ceil(depth * fraction));
    }
    public boolean channel(int px, int pz) {
        if (kind != Kind.OVERFLOW) return false;
        double dx = (double)px - x, dz = (double)pz - z;
        for (int i = 0; i < outlets; i++) {
            double angle = heading + i * Math.PI * 2 / outlets;
            double along = dx * Math.cos(angle) + dz * Math.sin(angle);
            double across = dz * Math.cos(angle) - dx * Math.sin(angle);
            if (along >= 0 && along <= reach && Math.abs(across) < width / 2.0 + 0.5) return true;
        }
        return false;
    }
    public boolean shaft(int px, int pz) {
        if (kind != Kind.OUTLET) return false;
        for (int i = 0; i < outlets; i++) {
            double angle = heading + i * Math.PI * 2 / outlets;
            int sx = x + (int)Math.round(Math.cos(angle) * radius * 0.4);
            int sz = z + (int)Math.round(Math.sin(angle) * radius * 0.4);
            if (px >= sx && px < sx + width && pz >= sz && pz < sz + width) return true;
        }
        return false;
    }
    public boolean cuts(int px, int y, int pz) {
        return basin(px, pz) && y > bed(px, pz)
                || channel(px, pz) && y >= level - 1
                || shaft(px, pz) && y <= level;
    }
    public boolean source(int px, int y, int pz) {
        return y <= level && (basin(px, pz) && y > bed(px, pz) || channel(px, pz) && y >= level - 1);
    }
    public boolean lining(int px, int y, int pz) {
        if (shaft(px, pz)) return false;
        int bed = basin(px, pz) ? bed(px, pz) : channel(px, pz) ? level - 2 : Integer.MIN_VALUE;
        return bed != Integer.MIN_VALUE && y <= bed && y >= liningBottom(px, pz);
    }
    public int liningBottom(int px, int pz) {
        int bed = basin(px, pz) ? bed(px, pz) : channel(px, pz) ? level - 2 : level - 1;
        // A steep bowl's neighboring water can sit below this column's own floor.
        // Retain those vertical step faces as well as the horizontal bed.
        bed = Math.min(bed, neighboringBed(px - 1, pz));
        bed = Math.min(bed, neighboringBed(px + 1, pz));
        bed = Math.min(bed, neighboringBed(px, pz - 1));
        bed = Math.min(bed, neighboringBed(px, pz + 1));
        return bed - liningDepth + 1;
    }
    private int neighboringBed(int px, int pz) {
        return basin(px, pz) ? bed(px, pz) : channel(px, pz) ? level - 2 : level - 1;
    }
    public boolean affects(int px, int pz) {
        long dx = (long)px - x, dz = (long)pz - z;
        int reach = radius + Math.max(2, beachWidth);
        return dx * dx + dz * dz <= (long)reach * reach || channel(px, pz);
    }
    public boolean protects(int px, int y, int pz) {
        long dx = (long)px - x, dz = (long)pz - z;
        return lining(px, y, pz) || kind != Kind.PERIMETER && dx * dx + dz * dz <= (long)(radius + 2) * (radius + 2)
                && !basin(px, pz) && !channel(px, pz)
                && y <= level + 1 && y >= liningBottom(px, pz);
    }
}
