package design.inherent.createskycompat.placement;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class VillagePlanTest {
    @Test void plansRespectEveryCountAndIgnoreCandidateVisitOrder() {
        var candidates = new ArrayList<VillagePlan.Site>();
        for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) candidates.add(new VillagePlan.Site(x * 24, z * 24));
        for (int count = 0; count <= 3; count++) {
            long seed = 0; while (VillageDistribution.forAnchor(seed) != count) seed++;
            var anchor = new VillagePlan.Anchor(seed, 0, 0, 330, 350);
            var forward = VillagePlan.select(anchor, candidates, site -> anchor, 64, 512);
            assertEquals(count, forward.size());
            Collections.reverse(candidates);
            assertEquals(forward, VillagePlan.select(anchor, candidates, site -> anchor, 64, 512));
            assertThrows(UnsupportedOperationException.class, () -> forward.add(new VillagePlan.Site(0, 0)));
        }
    }

    @Test void eachSiteHasOneOwnerWithStableBoundaryTies() {
        var west = new VillagePlan.Anchor(1, -800, 8, 330, 300);
        var east = new VillagePlan.Anchor(2, 816, 8, 330, 500);
        var middle = new VillagePlan.Site(0, 0);
        assertEquals(west, VillagePlan.owner(middle, List.of(east, west)));
        assertEquals(west, VillagePlan.owner(middle, List.of(west, east)));
        assertNull(VillagePlan.owner(middle, List.of()));
        assertTrue(VillagePlan.select(east, List.of(middle), site -> west, 64, 512).isEmpty());
    }

    @Test void nearbyAndDistantSitesCannotFillAQuota() {
        long seed = 0; while (VillageDistribution.forAnchor(seed) != 3) seed++;
        var anchor = new VillagePlan.Anchor(seed, -1000, -1000, 330, 350);
        assertTrue(VillagePlan.select(anchor, List.of(new VillagePlan.Site(-63, -63), new VillagePlan.Site(10000, 10000)),
                site -> anchor, 64, 512).isEmpty());
    }
}
