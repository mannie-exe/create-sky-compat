package design.inherent.skyweavecompat.rendering;

import design.inherent.skyweavecompat.client.OpaqueDepthFrame;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OpaqueDepthFrameTest {
    @Test void requiresCurrentOpaquePassAndDoesNotPromoteStaleOrForeignDepth() {
        var state = new OpaqueDepthFrame(); var first = new Object(); var other = new Object();
        state.translucent(first, 7); assertFalse(state.valid(first, 7));
        state.opaque(first, 7); assertTrue(state.valid(first, 7)); assertFalse(state.snapshot());
        state.translucent(other, 7); assertFalse(state.snapshot());
        state.translucent(first, 8); assertFalse(state.snapshot());
        assertFalse(state.valid(first, 8));
        state.translucent(first, 7); assertTrue(state.snapshot());
        state.opaque(first, 8); assertFalse(state.snapshot()); assertTrue(state.valid(first, 8));
    }
    @Test void pipelineDisposalRejectsReusedFrameNumbers() {
        var state = new OpaqueDepthFrame(); var first = new Object(); var next = new Object();
        state.opaque(first, 0); state.clear(next); assertTrue(state.valid(first, 0));
        state.clear(first); assertFalse(state.valid(first, 0)); assertFalse(state.valid(next, 0));
        state.opaque(next, 0); assertTrue(state.valid(next, 0)); assertFalse(state.valid(first, 0));
    }
}
