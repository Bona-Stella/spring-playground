package com.github.stella.springmsamq.auth.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RedisTokenService {
    // 네이밍 통일: "auth:" 접두어로 그룹핑
    private static final String REFRESH_KEY_PREFIX = "auth:refresh:";
    private static final String BLACKLIST_KEY_PREFIX = "auth:blacklist:";

    private final StringRedisTemplate redis;

    public RedisTokenService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void saveRefreshToken(Long userId, String token, Duration ttl) {
        redis.opsForValue().set(REFRESH_KEY_PREFIX + userId, token, ttl);
    }

    public String getRefreshToken(Long userId) {
        return redis.opsForValue().get(REFRESH_KEY_PREFIX + userId);
    }

    public void deleteRefreshToken(Long userId) {
        redis.delete(REFRESH_KEY_PREFIX + userId);
    }

    public void blacklistAccess(String jti, Duration ttl) {
        // 값 "1" 대신 "blocked" 처럼 의미 있는 문자열을 넣거나, 날짜를 넣어도 됨
        redis.opsForValue().set(BLACKLIST_KEY_PREFIX + jti, "blocked", ttl);
    }

    // 사실 Auth 서비스 자체는 블랙리스트를 거의 확인 안 함 (Gateway가 함)
    // 하지만 방어 로직으로 가지고 있으면 좋음
    public boolean isAccessBlacklisted(String jti) {
        return Boolean.TRUE.equals(redis.hasKey(BLACKLIST_KEY_PREFIX + jti));
    }
}
