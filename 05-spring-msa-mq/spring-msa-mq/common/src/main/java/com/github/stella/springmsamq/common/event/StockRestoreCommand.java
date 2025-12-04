package com.github.stella.springmsamq.common.event;

import java.io.Serializable;

/**
 * 보상 트랜잭션: 재고 복원을 지시하는 커맨드
 */
public record StockRestoreCommand(
        Long orderId,
        Long productId,
        int quantity
) implements Serializable {}
