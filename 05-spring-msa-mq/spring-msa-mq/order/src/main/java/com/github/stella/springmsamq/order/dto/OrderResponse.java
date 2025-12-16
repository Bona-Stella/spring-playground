package com.github.stella.springmsamq.order.dto;

import com.github.stella.springmsamq.order.domain.PurchaseOrder;

public record OrderResponse(
        Long orderId,
        Long userId,
        Long productId,
        int quantity,
        int totalPrice,
        String status
) {
    // Entity -> DTO 변환 메서드
    public static OrderResponse from(PurchaseOrder order) {
        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getProductId(),
                order.getQuantity(),
                order.getTotalPrice(),
                order.getStatus().name()
        );
    }
}
