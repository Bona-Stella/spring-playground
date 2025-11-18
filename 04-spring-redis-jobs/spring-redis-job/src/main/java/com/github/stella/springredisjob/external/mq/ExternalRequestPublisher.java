package com.github.stella.springredisjob.external.mq;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class ExternalRequestPublisher {
    private final StringRedisTemplate stringRedisTemplate;

    public ExternalRequestPublisher(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void publish(String city) {
        stringRedisTemplate.convertAndSend("ext:requests", city);
    }
}
