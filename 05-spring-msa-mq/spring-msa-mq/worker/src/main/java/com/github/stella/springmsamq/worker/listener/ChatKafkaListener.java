package com.github.stella.springmsamq.worker.listener;

import com.github.stella.springmsamq.common.chat.ChatPayload;
import com.github.stella.springmsamq.common.chat.ChatTopics;
import com.github.stella.springmsamq.worker.chat.ChatLog;
import com.github.stella.springmsamq.worker.chat.ChatLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class ChatKafkaListener {
    private static final Logger log = LoggerFactory.getLogger(ChatKafkaListener.class);

    private final ChatLogRepository repository;

    public ChatKafkaListener(ChatLogRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(topics = ChatTopics.KAFKA_TOPIC, groupId = "chat-archiver")
    public void onChatMessage(@Payload ChatPayload payload) {
        log.info("[Worker] Kafka consume chat: room={}, sender={}, content={}", payload.roomId(), payload.sender(), payload.content());
        repository.save(new ChatLog(payload.roomId(), payload.sender(), payload.content(), payload.timestamp()));
    }
}
