package com.github.stella.springmsamq.chat.web;

import com.github.stella.springmsamq.chat.dto.ChatMessage;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class ChatMessageController {

    private final SimpMessagingTemplate messagingTemplate;

    public ChatMessageController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat.send")
    public void send(ChatMessage message) {
        long ts = message.timestamp() > 0 ? message.timestamp() : System.currentTimeMillis();
        ChatMessage enriched = new ChatMessage(message.roomId(), message.sender(), message.content(), ts);
        String destination = "/topic/room." + enriched.roomId();
        messagingTemplate.convertAndSend(destination, enriched);
    }
}
