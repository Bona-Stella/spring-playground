package com.github.stella.springsecurityjwt.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class RedisTokenService {
    private final StringRedisTemplate redis;

    public RedisTokenService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void storeRefreshToken(String username, String refreshToken, long ttlSeconds) {
        String key = refreshKey(username);
        redis.opsForValue().set(key, refreshToken, Duration.ofSeconds(ttlSeconds));
    }

    public Optional<String> getRefreshToken(String username) {
        return Optional.ofNullable(redis.opsForValue().get(refreshKey(username)));
    }

    public void deleteRefreshToken(String username) {
        redis.delete(refreshKey(username));
    }

    public void blacklistAccessToken(String token, long ttlSeconds) {
        String key = blacklistKey(token);
        redis.opsForValue().set(key, "1", Duration.ofSeconds(ttlSeconds));
    }

    public boolean isBlacklisted(String token) {
        Boolean has = redis.hasKey(blacklistKey(token));
        return has != null && has;
    }

    private String refreshKey(String username) { return "auth:refresh:" + username; }
    private String blacklistKey(String token) { return "auth:blacklist:" + token; }
}
