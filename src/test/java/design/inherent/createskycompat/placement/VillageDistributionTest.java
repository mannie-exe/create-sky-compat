package design.inherent.createskycompat.placement;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VillageDistributionTest {
    @Test void exactLogarithmicWeightsAndBoundaries() {
        int[] histogram = new int[4];
        for (int ticket = 0; ticket < VillageDistribution.TICKETS; ticket++) histogram[VillageDistribution.forTicket(ticket)]++;
        assertArrayEquals(new int[]{96, 24, 8, 3}, histogram);
        assertThrows(IllegalArgumentException.class, () -> VillageDistribution.forTicket(-1));
        assertThrows(IllegalArgumentException.class, () -> VillageDistribution.forTicket(131));
    }
    @Test void seedSelectionIsBoundedAndRepeatable() {
        for (long seed : new long[]{0, 1, -1, Long.MIN_VALUE, Long.MAX_VALUE}) {
            int count = VillageDistribution.forAnchor(seed);
            assertTrue(count >= 0 && count <= 3);
            assertEquals(count, VillageDistribution.forAnchor(seed));
        }
    }
}
