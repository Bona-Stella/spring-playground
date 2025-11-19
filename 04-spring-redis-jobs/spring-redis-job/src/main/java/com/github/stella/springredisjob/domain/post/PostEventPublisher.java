package com.github.stella.springredisjob.domain.post;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class PostEventPublisher {
    private final StringRedisTemplate stringRedisTemplate;

    public PostEventPublisher(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void publishCreate(String title, String content) {
        // 매우 단순한 포맷: "CREATE|title|content" (데모 목적)
        String payload = String.join("|", "CREATE", title, content);
        stringRedisTemplate.convertAndSend("post:events", payload);
    }
}
