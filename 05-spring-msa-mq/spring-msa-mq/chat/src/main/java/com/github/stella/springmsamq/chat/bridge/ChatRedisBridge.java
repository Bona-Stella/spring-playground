package com.github.stella.springmsamq.chat.bridge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.stella.springmsamq.common.chat.ChatPayload;
import com.github.stella.springmsamq.common.chat.ChatTopics;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class ChatRedisBridge implements MessageListener {
    private static final Logger log = LoggerFactory.getLogger(ChatRedisBridge.class);

    private final RedisMessageListenerContainer container;
    private final ChannelTopic topic;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public ChatRedisBridge(RedisMessageListenerContainer container,
                           ChannelTopic topic,
                           @Lazy SimpMessagingTemplate messagingTemplate) {
        this.container = container;
        this.topic = topic;
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void subscribe() {
        container.addMessageListener(this, topic);
        log.info("[Chat] Subscribed to Redis channel: {}", ChatTopics.REDIS_CHANNEL);
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String payloadStr = new String(message.getBody());
            ChatPayload payload = objectMapper.readValue(payloadStr, ChatPayload.class);
            String destination = "/topic/room." + payload.roomId();
            messagingTemplate.convertAndSend(destination, payload);
        } catch (Exception e) {
            log.error("[Chat] Failed to process Redis chat message", e);
        }
    }
}
