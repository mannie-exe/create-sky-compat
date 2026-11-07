package design.inherent.skyweavecompat.terrain.city;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CityClearanceTest {
    private final CityClearance.Piece low = new CityClearance.Piece(new StructureBurial.Box(0, 0, 0, 16, 30, 16), 1);

    @Test void separatePiecesDoNotExcavateTheEntireLayoutEnvelope() {
        var far = new CityClearance.Piece(new StructureBurial.Box(80, 0, 0, 96, 30, 16), 1);
        var pieces = List.of(low, far);
        assertTrue(CityClearance.influence(pieces, 8, 8).contains(20));
        assertFalse(CityClearance.influence(pieces, 45, 8).contains(20));
        assertFalse(CityClearance.influence(pieces, -13, 8).contains(20));
        assertEquals(CityClearance.influence(pieces, 8, 8), CityClearance.influence(List.of(far, low), 8, 8));
    }

    @Test void everyPieceRetainsItsFoundationEvenWhenAnotherPieceOverlaps() {
        var elevated = new CityClearance.Piece(new StructureBurial.Box(4, 10, 4, 12, 25, 12), 11);
        var pieces = List.of(low, elevated);
        for (int y : new int[]{-3, 0, 1, 7, 10, 11}) assertTrue(CityClearance.protectsFloor(pieces, 8, y, 8));
        assertFalse(CityClearance.protectsFloor(pieces, 8, 5, 8));
        assertFalse(CityClearance.protectsFloor(pieces, 18, 1, 8));
    }

    @Test void shellUsesNeighboringBodyBoundsRatherThanOnlyTheCenterColumn() {
        var shell = CityClearance.retainShell(0, 0, 12, (x,z) -> x == 3 && z == 2
                ? new CityClearance.Range(10, 80) : new CityClearance.Range(-20, 100));
        assertEquals(new CityClearance.Range(22, 68), shell);
        assertFalse(shell.contains(21)); assertFalse(shell.contains(69));
        assertFalse(CityClearance.retainShell(0, 0, 12, (x,z) -> x == 3 && z == 2
                ? CityClearance.Range.EMPTY : new CityClearance.Range(-20, 100)).contains(50));
    }

    @Test void positiveOrWeakDensityDoesNotBecomeAnUnconditionalAirMask() {
        assertTrue(CityClearance.clears(-.55));
        assertFalse(CityClearance.clears(-.2)); assertFalse(CityClearance.clears(.55));
        assertFalse(CityClearance.clears(Double.NaN)); assertFalse(CityClearance.clears(Double.NEGATIVE_INFINITY));
    }

    @Test void aBuriedCoreStillNeedsItsOwnFootingAndRetainedShell() {
        var body = new CityClearance.Range(0, 120);
        assertTrue(CityClearance.coreFits(body, 40, 80, 12, true));
        assertFalse(CityClearance.coreFits(body, 40, 80, 12, false));
        assertFalse(CityClearance.coreFits(body, 10, 80, 12, true));
        assertFalse(CityClearance.coreFits(body, 40, 110, 12, true));
    }
}
