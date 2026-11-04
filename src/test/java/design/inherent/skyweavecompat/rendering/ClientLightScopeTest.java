package design.inherent.skyweavecompat.rendering;

import design.inherent.skyweavecompat.lighting.ClientLightScope;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ClientLightScopeTest {
    @Test void nestedFailureRestoresTheOuterScopeAndDoesNotCrossThreads() throws Exception {
        assertFalse(ClientLightScope.serverQuery());
        Boolean outer = ClientLightScope.enterServer();
        try {
            Boolean inner = ClientLightScope.enterServer();
            try { assertTrue(ClientLightScope.serverQuery()); throw new IllegalStateException("fixture"); }
            catch (IllegalStateException expected) { assertEquals("fixture", expected.getMessage()); }
            finally { ClientLightScope.restore(inner); }
            assertTrue(ClientLightScope.serverQuery());
            var otherThread = new AtomicBoolean(true);
            var thread = new Thread(() -> otherThread.set(ClientLightScope.serverQuery()));
            thread.start(); thread.join();
            assertFalse(otherThread.get());
        } finally { ClientLightScope.restore(outer); }
        assertFalse(ClientLightScope.serverQuery());
    }
}
