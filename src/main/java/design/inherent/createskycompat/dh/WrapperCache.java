package design.inherent.createskycompat.dh;

import java.lang.ref.WeakReference;
import java.util.Map;

/** Publish a replacement before releasing its predecessor; cleanup must retain a newer owner. */
public final class WrapperCache {
    private WrapperCache() {}

    public static <K, V> WeakReference<V> exchange(Map<K, WeakReference<V>> cache, K key, V replacement) {
        synchronized (cache) {
            var previous = cache.get(key);
            if (previous == null || previous.get() != replacement) cache.put(key, new WeakReference<>(replacement));
            return previous;
        }
    }

    public static <K, V> WeakReference<V> removeOwned(Map<K, WeakReference<V>> cache, K key, V owner) {
        synchronized (cache) {
            var current = cache.get(key);
            return current != null && current.get() == owner ? cache.remove(key) : null;
        }
    }
}
