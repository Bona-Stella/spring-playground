package com.github.stella.springmsamq.chat.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.stella.springmsamq.chat.dto.ChatMessage;
import com.github.stella.springmsamq.common.chat.ChatPayload;
import com.github.stella.springmsamq.common.chat.ChatTopics;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatService chatService; // 서비스만 바라봄

    @MessageMapping("/chat.send")
    public void send(ChatMessage message) {
        chatService.processMessage(message); // "서비스야 처리해줘" 끝.
    }
}
