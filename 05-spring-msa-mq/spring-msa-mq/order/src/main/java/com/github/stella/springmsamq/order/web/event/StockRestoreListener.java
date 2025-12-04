package com.github.stella.springmsamq.order.web.event;

import com.github.stella.springmsamq.common.event.OrderAmqp;
import com.github.stella.springmsamq.common.event.StockRestoreCommand;
import com.github.stella.springmsamq.order.domain.PurchaseOrder;
import com.github.stella.springmsamq.order.repo.PurchaseOrderRepository;
import com.github.stella.springmsamq.order.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class StockRestoreListener {
    private static final Logger log = LoggerFactory.getLogger(StockRestoreListener.class);

    private final ProductService productService;
    private final PurchaseOrderRepository orderRepository;

    public StockRestoreListener(ProductService productService, PurchaseOrderRepository orderRepository) {
        this.productService = productService;
        this.orderRepository = orderRepository;
    }

    @Transactional
    @RabbitListener(queues = OrderAmqp.QUEUE_STOCK_RESTORE)
    public void onStockRestore(@Payload StockRestoreCommand cmd) {
        log.warn("[Order] Compensation triggered: restore stock, orderId={}, productId={}, qty={}",
                cmd.orderId(), cmd.productId(), cmd.quantity());
        // 재고 복원
        productService.incrementStock(cmd.productId(), cmd.quantity());
        // 주문 상태 취소로 마킹
        orderRepository.findById(cmd.orderId()).ifPresent(po -> {
            po.cancel();
            orderRepository.save(po);
        });
    }
}
