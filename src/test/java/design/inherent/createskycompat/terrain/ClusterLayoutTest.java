package design.inherent.createskycompat.terrain;

import java.util.*;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ClusterLayoutTest {
    private static ClusterLayout.Candidate candidate(int cellX, int cellZ, long rank, ClusterLayout.Support... supports) {
        return new ClusterLayout.Candidate(new ClusterLayout.Cell(cellX, cellZ), rank, List.of(supports));
    }

    @Test void comparesIndividualSupportsIncludingCompanionsAndExactGap() {
        var a = candidate(-1, 0, 1, new ClusterLayout.Support(-800, 0, 300), new ClusterLayout.Support(-50, 0, 100));
        var b = candidate(0, 0, 2, new ClusterLayout.Support(800, 0, 300), new ClusterLayout.Support(500, 0, 100));
        assertFalse(ClusterLayout.conflicts(a, b, 350));
        assertTrue(ClusterLayout.conflicts(a, b, 351));
        // Filled cluster circles overlap here, but their individual islands remain separated.
        assertTrue(Math.abs(-800 - 800) < 2 * GenerationProfile.INITIAL.clusterReach() + 350);
    }

    @Test void priorityTiesAreResolvedWithoutRecursionOrVisitOrder() {
        var a = candidate(-1, 0, 0, new ClusterLayout.Support(-100, 0, 300));
        var b = candidate(0, 0, 0, new ClusterLayout.Support(500, 0, 300));
        var map = Map.of(a.cell(), a, b.cell(), b);
        ClusterLayout.Candidates source = (x, z) -> map.get(new ClusterLayout.Cell(x, z));
        assertTrue(ClusterLayout.accepts(a, GenerationProfile.INITIAL, source));
        assertFalse(ClusterLayout.accepts(b, GenerationProfile.INITIAL, source));
    }

    @Test void boundedLookupsMatchAnExhaustiveConflictCheckAcrossNegativeCellsAndParallelQueries() {
        var p = GenerationProfile.INITIAL;
        var random = new Random(42);
        var map = new HashMap<ClusterLayout.Cell, ClusterLayout.Candidate>();
        for (int x = -8; x <= 8; x++) for (int z = -8; z <= 8; z++) {
            int cx = x * p.spacing() + p.spacing() / 2 + random.nextInt(-p.jitter(), p.jitter() + 1);
            int cz = z * p.spacing() + p.spacing() / 2 + random.nextInt(-p.jitter(), p.jitter() + 1);
            int radius = random.nextInt(p.anchorMinRadius(), p.anchorMaxRadius() + 1);
            var c = candidate(x, z, random.nextLong(), new ClusterLayout.Support(cx, cz, GenerationProfile.supportRadius(radius)),
                    new ClusterLayout.Support(cx + p.companionMaxOrbit(), cz, GenerationProfile.supportRadius(p.companionMaxRadius())));
            map.put(c.cell(), c);
        }
        ClusterLayout.Candidates source = (x, z) -> map.get(new ClusterLayout.Cell(x, z));
        var rows = map.values().stream().toList();
        var serial = rows.stream().map(c -> ClusterLayout.accepts(c, p, source)).toList();
        var parallel = rows.parallelStream().map(c -> ClusterLayout.accepts(c, p, source)).toList();
        assertEquals(serial, parallel);
        for (int i = 0; i < rows.size(); i++) {
            var c = rows.get(i);
            boolean exhaustive = rows.stream().noneMatch(other -> other != c
                    && Long.compareUnsigned(other.priority(), c.priority()) < 0
                    && ClusterLayout.conflicts(c, other, p.minimumClusterGap()));
            assertEquals(exhaustive, serial.get(i));
            for (var support : c.supports()) {
                int supportCellX = Math.floorDiv(support.x(), p.spacing());
                assertTrue(Math.abs(supportCellX - c.cell().x()) <= p.lookupCells());
            }
        }
        var accepted = IntStream.range(0, rows.size()).filter(serial::get).mapToObj(rows::get).toList();
        for (var a : accepted) for (var b : accepted) if (a != b) assertFalse(ClusterLayout.conflicts(a, b, p.minimumClusterGap()));
    }
}
