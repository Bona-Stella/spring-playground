package com.github.stella.springredisjob.common.lock;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Simple abstraction for a distributed lock provider.
 */
public interface DistributedLock {
    boolean lock(String key, Duration ttl);
    void unlock(String key);

    /**
     * Optional override for wait seconds when acquiring the lock.
     * Default implementation delegates to {@link #lock(String, Duration)}.
     */
    default boolean lock(String key, Duration ttl, Long waitSecondsOverride) {
        return lock(key, ttl);
    }

    default void withLock(String key, Duration ttl, Runnable action) {
        if (lock(key, ttl)) {
            try { action.run(); } finally { unlock(key); }
        }
    }

    default <T> T withLock(String key, Duration ttl, Supplier<T> supplier, T fallback) {
        if (lock(key, ttl)) {
            try { return supplier.get(); } finally { unlock(key); }
        }
        return fallback;
    }
}
