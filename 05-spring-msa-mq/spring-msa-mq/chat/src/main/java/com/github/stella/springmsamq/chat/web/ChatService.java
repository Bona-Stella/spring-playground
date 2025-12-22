package com.github.stella.springmsamq.chat.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.stella.springmsamq.chat.dto.ChatMessage;
import com.github.stella.springmsamq.common.chat.ChatPayload;
import com.github.stella.springmsamq.common.chat.ChatTopics;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String, ChatPayload> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void processMessage(ChatMessage message) {
        // 1. 로직 처리 (타임스탬프 등)
        long ts = message.timestamp() > 0 ? message.timestamp() : System.currentTimeMillis();
        ChatPayload payload = new ChatPayload(message.roomId(), message.sender(), message.content(), ts);

        // 2. Redis 발행 (실시간)
        try {
            String json = objectMapper.writeValueAsString(payload);
            redisTemplate.convertAndSend(ChatTopics.REDIS_CHANNEL, json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("메시지 변환 실패", e);
        }

        // 3. Kafka 발행 (아카이빙)
        kafkaTemplate.send(ChatTopics.KAFKA_TOPIC, payload.roomId(), payload);
    }
}
