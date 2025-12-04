package com.github.stella.springmsamq.chat.dto;

public record ChatMessage(
        String roomId,
        String sender,
        String content,
        long timestamp
) {}
