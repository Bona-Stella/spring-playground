package com.github.stella.springmsamq.gateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.stella.springmsamq.common.auth.AuthTopics;
import com.github.stella.springmsamq.common.auth.RevokeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class RevokeSubscriber implements MessageListener {
    private static final Logger log = LoggerFactory.getLogger(RevokeSubscriber.class);

    private final RedisMessageListenerContainer container;
    private final ChannelTopic topic;
    private final InMemoryDenyList denyList;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RevokeSubscriber(RedisMessageListenerContainer container,
                            ChannelTopic revokeTopic,
                            @Lazy InMemoryDenyList denyList) {
        this.container = container;
        this.topic = revokeTopic;
        this.denyList = denyList;
    }

    @PostConstruct
    public void subscribe() {
        container.addMessageListener(this, topic);
        log.info("[Gateway] Subscribed to revoke channel: {}", AuthTopics.REDIS_CHANNEL_REVOKE);
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String json = new String(message.getBody());
            RevokeToken revoke = objectMapper.readValue(json, RevokeToken.class);
            denyList.put(revoke.jti(), revoke.expEpochMillis());
            log.info("[Gateway] Revoked jti={}, exp={}", revoke.jti(), revoke.expEpochMillis());
        } catch (Exception e) {
            log.error("[Gateway] Failed to process revoke message", e);
        }
    }
}
