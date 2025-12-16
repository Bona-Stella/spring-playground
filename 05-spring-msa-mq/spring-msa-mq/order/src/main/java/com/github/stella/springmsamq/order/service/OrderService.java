package com.github.stella.springmsamq.order.service;

import com.github.stella.springmsamq.order.domain.Product;
import com.github.stella.springmsamq.order.domain.PurchaseOrder;
import com.github.stella.springmsamq.order.web.event.OrderEventPublisher;
import com.github.stella.springmsamq.order.repo.PurchaseOrderRepository;
import com.github.stella.springmsamq.common.lock.DistributedLock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final ProductService productService;
    private final PurchaseOrderRepository orderRepository;

    /**
     * [리팩토링 포인트 3] 순수 트랜잭션 로직
     * Facade에서 호출하므로, 이미 Lock이 걸려있는 상태에서 안전하게 진입합니다.
     */
    @Transactional
    public PurchaseOrder createOrderInternal(Long userId, Long productId, int quantity) {
        // 1. 상품 조회
        Product product = productService.findById(productId);

        // 2. 가격 계산 및 재고 검증
        // (Product 엔티티에 비즈니스 로직이 있다면 product.calculateTotal(quantity) 등으로 위임 가능)
        int total = product.getPrice() * quantity;

        // 3. 재고 차감 (동시성 제어는 Facade의 Lock이 보장해줌)
        productService.decrementStock(productId, quantity);

        // 4. 주문 생성
        PurchaseOrder order = new PurchaseOrder(
                null,
                userId,
                productId,
                quantity,
                total,
                LocalDateTime.now()
        );

        // 5. 저장 및 반환
        return orderRepository.save(order);
    }
}
