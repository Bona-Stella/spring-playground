package com.github.stella.springredisjob.common.lock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Legacy simple Redis SET NX PX based lock.
 * Enabled when app.lock.provider=plain (default off).
 */
@Deprecated
@Component
@ConditionalOnProperty(name = "app.lock.provider", havingValue = "plain")
public class RedisLockService implements DistributedLock {
    private final StringRedisTemplate stringRedisTemplate;

    public RedisLockService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean lock(String key, Duration ttl) {
        Boolean ok = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", ttl);
        return Boolean.TRUE.equals(ok);
    }

    @Override
    public boolean lock(String key, Duration ttl, Long waitSecondsOverride) {
        // Plain Redis SET NX PX에는 대기 개념이 없어 즉시 시도만 수행
        return lock(key, ttl);
    }

    @Override
    public void unlock(String key) {
        stringRedisTemplate.delete(key);
    }
}
