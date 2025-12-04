package com.github.stella.springmsamq.worker.listener;

import com.github.stella.springmsamq.common.event.OrderAmqp;
import com.github.stella.springmsamq.common.event.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedListener {
    private static final Logger log = LoggerFactory.getLogger(OrderCreatedListener.class);

    @RabbitListener(queues = OrderAmqp.QUEUE_CREATED)
    public void onOrderCreated(@Payload OrderCreatedEvent event) {
        log.info("[Worker] Consumed OrderCreatedEvent: orderId={}, userId={}, productId={}, quantity={}, totalPrice={}, createdAt={}",
                event.orderId(), event.userId(), event.productId(), event.quantity(), event.totalPrice(), event.createdAt());
        // TODO: payment/notification processing (simulate)
    }
}
