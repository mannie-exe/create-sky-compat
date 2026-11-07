package design.inherent.skyweavecompat.terrain.city;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import design.inherent.skyweavecompat.terrain.city.StructureBurial.Box;

class StructureBurialTest {
    @Test void overlappingPiecesAreUnionWeightedAndDuplicatesDoNotChangeTheMetric() {
        var a = new Box(0, 0, 0, 20, 10, 10);
        var b = new Box(10, 0, 0, 30, 10, 10);
        var plan = StructureBurial.plan(List.of(a, b));
        assertEquals(3000, plan.unionVolumeEstimate(), 8);
        assertEquals(2.0 / 3, plan.buriedFraction(0, (x,y,z) -> x < 20), .004);
        assertEquals(plan, StructureBurial.plan(List.of(b, a, a)));
    }

    @Test void smallPiecesHaveVolumeWeightRatherThanEqualVotes() {
        var plan = StructureBurial.plan(List.of(new Box(0,0,0,100,10,10), new Box(100,0,0,101,1,1)));
        assertEquals(10000.0 / 10001, plan.buriedFraction(0, (x,y,z) -> x < 100), .00001);
        assertEquals(4096, plan.samples().size());
    }

    @Test void shiftedBoundaryAndThinPeripheralExposureAgreeWithADenserReference() {
        var boxes = List.of(new Box(-80,-8,-30,80,22,30), new Box(40,-4,-60,100,16,60),
                new Box(-110,0,-4,-50,5,4));
        var standard = StructureBurial.plan(boxes);
        var dense = StructureBurial.plan(boxes, 32768);
        for (int delta : new int[]{-20,0,18}) {
            StructureBurial.Body body = (x,y,z) -> y >= -30 && y < 45 && x*x + z*z < 95*95;
            assertEquals(dense.buriedFraction(delta, body), standard.buriedFraction(delta, body), .012);
        }
        assertEquals(1, standard.buriedFraction(0, (x,y,z) -> true), 1e-12);
        assertEquals(0, standard.buriedFraction(0, (x,y,z) -> false));
    }

    @Test void invalidAndUnboundedLayoutsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Box(0,0,0,0,1,1));
        assertThrows(IllegalArgumentException.class, () -> StructureBurial.plan(List.of()));
        var boxes = new ArrayList<Box>();
        for (int x = 0; x <= StructureBurial.MAX_PIECES; x++) boxes.add(new Box(x,0,0,x+1,1,1));
        assertThrows(IllegalArgumentException.class, () -> StructureBurial.plan(boxes));
    }
}
