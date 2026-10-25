package design.inherent.createskycompat.placement;

/** Truncated logarithmic-series count: P(n) is proportional to (1/2)^(n+1)/(n+1). */
public final class VillageDistribution {
    public static final int TICKETS = 131;
    private VillageDistribution() {}

    public static int forAnchor(long seed) {
        long mixed = seed ^ 0x8C3C010CB4754C91L;
        mixed = (mixed ^ (mixed >>> 30)) * 0xBF58476D1CE4E5B9L;
        mixed = (mixed ^ (mixed >>> 27)) * 0x94D049BB133111EBL;
        mixed ^= mixed >>> 31;
        return forTicket((int)Long.remainderUnsigned(mixed, TICKETS));
    }

    public static int forTicket(int ticket) {
        if (ticket < 0 || ticket >= TICKETS) throw new IllegalArgumentException("Village ticket outside 0..130");
        if (ticket < 96) return 0;
        if (ticket < 120) return 1;
        if (ticket < 128) return 2;
        return 3;
    }
}
