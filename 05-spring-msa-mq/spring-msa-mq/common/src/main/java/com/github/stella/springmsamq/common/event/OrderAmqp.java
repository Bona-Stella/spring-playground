package com.github.stella.springmsamq.common.event;

public final class OrderAmqp {
    private OrderAmqp() {}
    public static final String EXCHANGE = "orders.exchange";
    public static final String ROUTING_KEY_CREATED = "orders.created";
    public static final String QUEUE_CREATED = "orders.created.queue";
}
