package com.github.stella.springmsamq.order.web.event;

import com.github.stella.springmsamq.common.event.OrderAmqp;
import com.github.stella.springmsamq.common.event.OrderCreatedEvent;
import com.github.stella.springmsamq.order.domain.PurchaseOrder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public OrderEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishOrderCreated(PurchaseOrder order) {
        OrderCreatedEvent event = new OrderCreatedEvent(
                order.getId(),
                order.getUserId(),
                order.getProductId(),
                order.getQuantity(),
                order.getTotalPrice(),
                order.getCreatedAt()
        );
        rabbitTemplate.convertAndSend(OrderAmqp.EXCHANGE, OrderAmqp.ROUTING_KEY_CREATED, event);
    }
}
