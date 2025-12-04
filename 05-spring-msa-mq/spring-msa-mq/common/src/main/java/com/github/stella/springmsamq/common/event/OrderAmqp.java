package com.github.stella.springmsamq.common.event;

public final class OrderAmqp {
    private OrderAmqp() {}
    public static final String EXCHANGE = "orders.exchange";
    public static final String ROUTING_KEY_CREATED = "orders.created";
    public static final String QUEUE_CREATED = "orders.created.queue";
    public static final String QUEUE_CREATED_RETRY = "orders.created.retry";
    // 보상(Compensation)용: 재고 복원 커맨드
    public static final String ROUTING_KEY_STOCK_RESTORE = "orders.stock.restore";
    public static final String QUEUE_STOCK_RESTORE = "orders.stock.restore.queue";
}
