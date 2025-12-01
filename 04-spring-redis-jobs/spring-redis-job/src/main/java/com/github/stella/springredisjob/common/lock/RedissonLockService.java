package com.github.stella.springredisjob.common.lock;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Redisson-based distributed lock implementation.
 * Enabled when app.lock.provider=redisson (default from application.properties).
 */
@Primary
@Component
@ConditionalOnProperty(name = "app.lock.provider", havingValue = "redisson", matchIfMissing = false)
public class RedissonLockService implements DistributedLock {

    private final RedissonClient redissonClient;
    private final long defaultWaitSeconds;

    public RedissonLockService(
            RedissonClient redissonClient,
            @Value("${app.lock.waitSeconds:2}") long defaultWaitSeconds
    ) {
        this.redissonClient = redissonClient;
        this.defaultWaitSeconds = defaultWaitSeconds;
    }

    @Override
    public boolean lock(String key, Duration ttl) {
        RLock lock = redissonClient.getLock(key);
        long leaseSec = Math.max(1, ttl == null ? 10 : ttl.toSeconds());
        try {
            return lock.tryLock(Math.max(0, defaultWaitSeconds), leaseSec, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public boolean lock(String key, Duration ttl, Long waitSecondsOverride) {
        RLock lock = redissonClient.getLock(key);
        long leaseSec = Math.max(1, ttl == null ? 10 : ttl.toSeconds());
        long waitSec = waitSecondsOverride == null ? Math.max(0, defaultWaitSeconds) : Math.max(0, waitSecondsOverride);
        try {
            return lock.tryLock(waitSec, leaseSec, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public void unlock(String key) {
        RLock lock = redissonClient.getLock(key);
        if (lock.isHeldByCurrentThread()) {
            try {
                lock.unlock();
            } catch (IllegalMonitorStateException ignored) {
                // ignore if not held
            }
        }
    }
}
