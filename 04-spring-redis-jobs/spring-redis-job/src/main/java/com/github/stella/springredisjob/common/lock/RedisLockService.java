package com.github.stella.springredisjob.common.lock;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RedisLockService {
    private final StringRedisTemplate stringRedisTemplate;

    public RedisLockService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public boolean lock(String key, Duration ttl) {
        Boolean ok = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, "1", ttl);
        return Boolean.TRUE.equals(ok);
    }

    public void unlock(String key) {
        stringRedisTemplate.delete(key);
    }
}
