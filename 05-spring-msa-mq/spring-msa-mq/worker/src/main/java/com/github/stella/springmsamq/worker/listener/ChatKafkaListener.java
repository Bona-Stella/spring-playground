package com.github.stella.springmsamq.worker.listener;

import com.github.stella.springmsamq.common.chat.ChatPayload;
import com.github.stella.springmsamq.common.chat.ChatTopics;
import com.github.stella.springmsamq.worker.chat.ChatLog;
import com.github.stella.springmsamq.worker.chat.ChatLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatKafkaListener {

    private final ChatLogRepository repository;

    @KafkaListener(topics = ChatTopics.KAFKA_TOPIC, groupId = "chat-archiver")
    public void onChatMessage(@Payload ChatPayload payload) {
        // 로그 레벨 조정: 대량의 채팅이 들어올 경우 info 로그는 디스크 I/O에 부담이 될 수 있음 (debug 권장)
        log.debug("[Worker] Kafka consume chat: room={}, sender={}", payload.roomId(), payload.sender());

        // Entity 변환 및 저장
        ChatLog chatLog = ChatLog.builder()
                .roomId(payload.roomId())
                .sender(payload.sender())
                .content(payload.content())
                .timestamp(payload.timestamp())
                .build();

        repository.save(chatLog);
    }
}
