package design.inherent.createskycompat.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorldTextScopeTest {
    @Test void restoresNestedSelectionAfterDrawFailure() {
        assertFalse(WorldTextScope.active());
        WorldTextScope.render(() -> {
            assertTrue(WorldTextScope.active());
            assertThrows(IllegalStateException.class, () -> WorldTextScope.render(() -> {
                assertTrue(WorldTextScope.active());
                throw new IllegalStateException("draw failed");
            }));
            assertTrue(WorldTextScope.active());
        });
        assertFalse(WorldTextScope.active());
        assertThrows(IllegalArgumentException.class, () -> WorldTextScope.render(() -> { throw new IllegalArgumentException(); }));
        assertFalse(WorldTextScope.active());
    }
}
