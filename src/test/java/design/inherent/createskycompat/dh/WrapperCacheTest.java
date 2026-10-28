package design.inherent.createskycompat.dh;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WrapperCacheTest {
    @Test void concurrentReadersClaimThePredecessorOnceAndDoNotWaitForCleanup() throws Exception {
        Map<String, WeakReference<Object>> cache = Collections.synchronizedMap(new HashMap<>());
        Object previous = new Object(), replacement = new Object(); cache.put("world", new WeakReference<>(previous));
        var start = new CountDownLatch(1);
        var cleaning = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> {
                start.await();
                var claimed = WrapperCache.exchange(cache, "world", replacement);
                cleaning.countDown(); release.await();
                WrapperCache.removeOwned(cache, "world", previous);
                return claimed.get();
            });
            start.countDown();
            try {
                assertTrue(cleaning.await(5, TimeUnit.SECONDS));
                var second = executor.submit(() -> WrapperCache.exchange(cache, "world", replacement).get());
                assertSame(replacement, second.get(5, TimeUnit.SECONDS));
            } finally { release.countDown(); }
            assertSame(previous, first.get(5, TimeUnit.SECONDS));
            assertSame(replacement, cache.get("world").get());
        }
    }

    @Test void delayedCleanupCannotRemoveASecondReplacement() {
        Map<String, WeakReference<Object>> cache = new HashMap<>();
        Object first = new Object(), second = new Object(), third = new Object();
        assertNull(WrapperCache.exchange(cache, "world", first));
        assertSame(first, WrapperCache.exchange(cache, "world", second).get());
        assertSame(second, WrapperCache.exchange(cache, "world", third).get());
        assertNull(WrapperCache.removeOwned(cache, "world", first));
        assertNull(WrapperCache.removeOwned(cache, "world", second));
        assertSame(third, WrapperCache.removeOwned(cache, "world", third).get());
        assertTrue(cache.isEmpty());
    }
}
