package com.github.stella.springmsamq.chat.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.stella.springmsamq.chat.dto.ChatMessage;
import com.github.stella.springmsamq.common.chat.ChatPayload;
import com.github.stella.springmsamq.common.chat.ChatTopics;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
public class ChatMessageController {

    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String, ChatPayload> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ChatMessageController(StringRedisTemplate redisTemplate,
                                 KafkaTemplate<String, ChatPayload> kafkaTemplate) {
        this.redisTemplate = redisTemplate;
        this.kafkaTemplate = kafkaTemplate;
    }

    @MessageMapping("/chat.send")
    public void send(ChatMessage message) throws JsonProcessingException {
        long ts = message.timestamp() > 0 ? message.timestamp() : System.currentTimeMillis();
        ChatPayload payload = new ChatPayload(message.roomId(), message.sender(), message.content(), ts);

        // 1) Redis Pub/Sub → 다른 인스턴스들에 브로드캐스트 트리거
        String json = objectMapper.writeValueAsString(payload);
        redisTemplate.convertAndSend(ChatTopics.REDIS_CHANNEL, json);

        // 2) Kafka 아카이빙
        kafkaTemplate.send(ChatTopics.KAFKA_TOPIC, payload.roomId(), payload);
    }
}
