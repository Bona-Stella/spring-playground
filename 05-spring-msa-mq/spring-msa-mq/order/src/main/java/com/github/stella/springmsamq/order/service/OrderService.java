package com.github.stella.springmsamq.order.service;

import com.github.stella.springmsamq.order.domain.Product;
import com.github.stella.springmsamq.order.domain.PurchaseOrder;
import com.github.stella.springmsamq.order.web.event.OrderEventPublisher;
import com.github.stella.springmsamq.order.repo.PurchaseOrderRepository;
import com.github.stella.springmsamq.common.lock.DistributedLock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class OrderService {
    private final ProductService productService;
    private final PurchaseOrderRepository orderRepository;
    private final OrderEventPublisher eventPublisher;

    public OrderService(ProductService productService, PurchaseOrderRepository orderRepository, OrderEventPublisher eventPublisher) {
        this.productService = productService;
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    @DistributedLock(key = "'order:product:' + #productId")
    public PurchaseOrder create(Long userId, Long productId, int quantity) {
        Product product = productService.findById(productId);
        int total = product.getPrice() * quantity;
        productService.decrementStock(productId, quantity);
        PurchaseOrder order = new PurchaseOrder(null, userId, productId, quantity, total, LocalDateTime.now());
        PurchaseOrder saved = orderRepository.save(order);
        eventPublisher.publishOrderCreated(saved);
        return saved;
    }
}
