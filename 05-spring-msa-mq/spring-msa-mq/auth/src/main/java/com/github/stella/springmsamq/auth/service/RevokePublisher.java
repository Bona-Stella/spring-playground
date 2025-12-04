package com.github.stella.springmsamq.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.stella.springmsamq.common.auth.AuthTopics;
import com.github.stella.springmsamq.common.auth.RevokeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RevokePublisher {
    private static final Logger log = LoggerFactory.getLogger(RevokePublisher.class);
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RevokePublisher(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void publish(String jti, long expEpochMillis) {
        try {
            String json = objectMapper.writeValueAsString(new RevokeToken(jti, expEpochMillis));
            redisTemplate.convertAndSend(AuthTopics.REDIS_CHANNEL_REVOKE, json);
            log.info("[Auth] Published revoke jti={}, exp={}", jti, expEpochMillis);
        } catch (JsonProcessingException e) {
            log.warn("[Auth] Failed to serialize revoke token", e);
        }
    }
}
