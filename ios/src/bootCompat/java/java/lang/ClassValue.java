package java.lang;

import java.util.Objects;
import java.util.WeakHashMap;

/**
 * MobiVM 3.0.0 compatibility implementation of {@link java.lang.ClassValue}.
 * MobiVM's Android libcore runtime omits {@code java.lang.ClassValue} (added in Android API 34),
 * which is required at runtime by Scala 2.12 {@code ClassTag} caching and Kotlin coroutines.
 */
public abstract class ClassValue<T> {
    private static final Object NULL_SENTINEL = new Object();

    private final Object lock = new Object();
    private final WeakHashMap<Class<?>, Entry> cache = new WeakHashMap<Class<?>, Entry>();
    private long version = 0L;

    private static final class Entry {
        final long version;
        final Object value;

        Entry(long version, Object value) {
            this.version = version;
            this.value = value;
        }
    }

    protected ClassValue() {
    }

    protected abstract T computeValue(Class<?> type);

    @SuppressWarnings("unchecked")
    public T get(Class<?> type) {
        Objects.requireNonNull(type);
        long observedVersion;
        synchronized (lock) {
            Entry existing = cache.get(type);
            if (existing != null) {
                return existing.value == NULL_SENTINEL ? null : (T) existing.value;
            }
            observedVersion = version;
        }

        // Compute outside the lock so re-entrant get() calls (e.g. array component ClassTags)
        // cannot deadlock or mutate WeakHashMap concurrently, retrying if remove() raced.
        while (true) {
            T computed = computeValue(type);
            Object masked = computed == null ? NULL_SENTINEL : computed;
            synchronized (lock) {
                Entry existing = cache.get(type);
                if (existing != null) {
                    return existing.value == NULL_SENTINEL ? null : (T) existing.value;
                }
                if (version == observedVersion) {
                    cache.put(type, new Entry(version, masked));
                    return computed;
                }
                observedVersion = version;
            }
        }
    }

    public void remove(Class<?> type) {
        Objects.requireNonNull(type);
        synchronized (lock) {
            version++;
            cache.remove(type);
        }
    }
}
