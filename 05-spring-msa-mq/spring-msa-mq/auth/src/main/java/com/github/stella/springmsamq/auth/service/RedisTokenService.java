package com.github.stella.springmsamq.auth.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RedisTokenService {
    private static final String REFRESH_KEY = "refresh_token:"; // refresh_token:{userId}
    private static final String BLACKLIST_KEY = "blacklist:access:"; // blacklist:access:{jti}

    private final StringRedisTemplate redis;

    public RedisTokenService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void saveRefreshToken(Long userId, String token, Duration ttl) {
        redis.opsForValue().set(REFRESH_KEY + userId, token, ttl);
    }

    public String getRefreshToken(Long userId) {
        return redis.opsForValue().get(REFRESH_KEY + userId);
    }

    public void deleteRefreshToken(Long userId) {
        redis.delete(REFRESH_KEY + userId);
    }

    public void blacklistAccess(String jti, Duration ttl) {
        redis.opsForValue().set(BLACKLIST_KEY + jti, "1", ttl);
    }

    public boolean isAccessBlacklisted(String jti) {
        String v = redis.opsForValue().get(BLACKLIST_KEY + jti);
        return v != null;
    }
}
