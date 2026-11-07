package design.inherent.skyweavecompat.terrain.city;

import java.util.*;

/** Deterministic, volume-weighted sampling of a union of half-open piece envelopes. */
public final class StructureBurial {
    public static final int SAMPLE_BUDGET = 4096;
    public static final int MAX_PIECES = 1024;
    public record Box(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        public Box {
            if (maxX <= minX || maxY <= minY || maxZ <= minZ) throw new IllegalArgumentException("Empty envelope");
        }
        public long volume() { return (long)(maxX - minX) * (maxY - minY) * (maxZ - minZ); }
        public boolean contains(int x, int y, int z) {
            return x >= minX && x < maxX && y >= minY && y < maxY && z >= minZ && z < maxZ;
        }
    }
    public record Sample(int x, int y, int z, double weight) {}
    @FunctionalInterface public interface Body { boolean contains(int x, int y, int z); }
    public record Plan(List<Sample> samples, double unionVolumeEstimate) {
        public Plan { samples = List.copyOf(samples); }
        public double buriedFraction(int deltaY, Body body) {
            double buried = 0;
            for (var sample : samples) if (body.contains(sample.x, sample.y + deltaY, sample.z)) buried += sample.weight;
            return unionVolumeEstimate == 0 ? 0 : buried / unionVolumeEstimate;
        }
    }
    private StructureBurial() {}

    public static Plan plan(List<Box> input) { return plan(input, SAMPLE_BUDGET); }

    /** The larger budget is used only by reference checks, not by production fitting. */
    public static Plan plan(List<Box> input, int budget) {
        var boxes = input.stream().distinct().sorted(Comparator.comparingInt(Box::minX).thenComparingInt(Box::minY)
                .thenComparingInt(Box::minZ).thenComparingInt(Box::maxX).thenComparingInt(Box::maxY).thenComparingInt(Box::maxZ)).toList();
        if (boxes.isEmpty() || boxes.size() > MAX_PIECES || budget < boxes.size() || budget > 65536)
            throw new IllegalArgumentException("Unsupported envelope sampling budget");
        long volume = boxes.stream().mapToLong(Box::volume).sum();
        int remaining = budget - boxes.size(), assigned = 0;
        int[] counts = new int[boxes.size()];
        var remainders = new ArrayList<Integer>();
        for (int i = 0; i < boxes.size(); i++) {
            counts[i] = 1 + (int)((double)remaining * boxes.get(i).volume() / volume);
            assigned += counts[i]; remainders.add(i);
        }
        remainders.sort(Comparator.<Integer>comparingDouble(i ->
                (double)remaining * boxes.get(i).volume() / volume - (counts[i] - 1)).reversed().thenComparingInt(i -> i));
        for (int i = 0; assigned < budget; i++, assigned++) counts[remainders.get(i)]++;
        var samples = new ArrayList<Sample>(budget);
        double union = 0;
        for (int piece = 0; piece < boxes.size(); piece++) {
            var box = boxes.get(piece);
            for (int n = 1; n <= counts[piece]; n++) {
                int x = box.minX + (int)(radicalInverse(n, 2) * (box.maxX - box.minX));
                int y = box.minY + (int)(radicalInverse(n, 3) * (box.maxY - box.minY));
                int z = box.minZ + (int)(radicalInverse(n, 5) * (box.maxZ - box.minZ));
                int owners = 0;
                for (var other : boxes) if (other.contains(x, y, z)) owners++;
                // Inverse multiplicity integrates union measure, not summed overlapping volume.
                double weight = (double)box.volume() / counts[piece] / owners;
                samples.add(new Sample(x, y, z, weight)); union += weight;
            }
        }
        return new Plan(samples, union);
    }

    private static double radicalInverse(int value, int base) {
        double result = 0, factor = 1.0 / base;
        while (value > 0) { result += (value % base) * factor; value /= base; factor /= base; }
        return result;
    }
}
