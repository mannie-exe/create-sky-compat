package design.inherent.createskycompat.climate;

import org.junit.jupiter.api.Test;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class SurfaceClimateScopeTest {
    @Test void nestingAndExceptionsRestorePreviousWorld() {
        assertFalse(SurfaceClimateScope.active());
        SurfaceClimateScope.run(true, () -> {
            assertTrue(SurfaceClimateScope.active());
            assertThrows(IllegalStateException.class, () -> SurfaceClimateScope.run(false, () -> {
                assertFalse(SurfaceClimateScope.active()); throw new IllegalStateException("fixture");
            }));
            assertTrue(SurfaceClimateScope.active());
        });
        assertFalse(SurfaceClimateScope.active());
    }

    @Test void concurrentWorldsDoNotShareContext() throws Exception {
        try (var executor = Executors.newSingleThreadExecutor()) {
            SurfaceClimateScope.run(true, () -> {
                try { assertFalse(executor.submit(SurfaceClimateScope::active).get()); }
                catch (Exception exception) { throw new AssertionError(exception); }
            });
        }
    }
}
