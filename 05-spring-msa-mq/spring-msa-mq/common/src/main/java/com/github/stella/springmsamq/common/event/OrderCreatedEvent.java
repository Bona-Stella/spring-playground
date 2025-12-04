package com.github.stella.springmsamq.common.event;

import java.io.Serializable;
import java.time.LocalDateTime;

public record OrderCreatedEvent(
        Long orderId,
        Long userId,
        Long productId,
        int quantity,
        int totalPrice,
        LocalDateTime createdAt
) implements Serializable {}
