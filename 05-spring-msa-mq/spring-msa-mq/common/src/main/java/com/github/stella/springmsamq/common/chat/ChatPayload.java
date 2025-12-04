package com.github.stella.springmsamq.common.chat;

public record ChatPayload(
        String roomId,
        String sender,
        String content,
        long timestamp
) {}
