package com.github.stella.springmsamq.order.service;

import com.github.stella.springmsamq.common.lock.DistributedLock;
import com.github.stella.springmsamq.order.domain.PurchaseOrder;
import com.github.stella.springmsamq.order.web.event.OrderEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderFacade {
    private final OrderService orderService;
    private final OrderEventPublisher eventPublisher;

    /**
     * [리팩토링 포인트 1] Lock의 범위를 트랜잭션보다 넓게 잡습니다.
     * 순서: Lock 획득 -> createOrderInternal(트랜잭션) -> Lock 해제
     */
    @DistributedLock(key = "'order:product:' + #productId")
    public PurchaseOrder createOrder(Long userId, Long productId, int quantity) {

        // 1. 핵심 비즈니스 로직 실행 (DB 트랜잭션은 이 메서드 안에서 시작되고 끝납니다)
        PurchaseOrder completedOrder = orderService.createOrderInternal(userId, productId, quantity);

        // 2. [리팩토링 포인트 2] 트랜잭션이 완전히 커밋된 후에 이벤트를 발행합니다.
        // DB 저장이 확실히 성공한 뒤에 메시지를 보내므로 '유령 주문'이 생기지 않습니다.
        try {
            eventPublisher.publishOrderCreated(completedOrder);
        } catch (Exception e) {
            // 메시지 발행 실패 시 로그를 남기거나, 재시도 로직(Retry)을 추가할 수 있습니다.
            // 하지만 이미 주문은 생성되었으므로, 비즈니스상으로는 '성공'으로 간주하거나
            // 별도의 '메시지 재발송 데몬'이 처리하게 둡니다.
            log.error("Failed to publish order event for orderId: {}", completedOrder.getId(), e);
        }

        return completedOrder;
    }
}
