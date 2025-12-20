package com.github.stella.springmsamq.worker.listener;

import com.github.stella.springmsamq.common.event.OrderAmqp;
import com.github.stella.springmsamq.common.event.OrderCreatedEvent;
import com.github.stella.springmsamq.common.event.StockRestoreCommand;
import com.github.stella.springmsamq.worker.config.WorkerProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedListener {

    private final RabbitTemplate rabbitTemplate;
    private final StringRedisTemplate redisTemplate;
    private final WorkerProperties props;
    private final MeterRegistry meterRegistry;

    // Metrics (생성자 주입 후 @PostConstruct 초기화 또는 지연 초기화 추천)
    private Counter processedCounter;
    private Counter skippedCounter;
    private Timer processingTimer;

    @PostConstruct
    public void initMetrics() {
        this.processedCounter = meterRegistry.counter("worker.orders.processed");
        this.skippedCounter = meterRegistry.counter("worker.orders.skipped.idempotent");
        this.processingTimer = meterRegistry.timer("worker.orders.processing.time");
    }

    @RabbitListener(queues = OrderAmqp.QUEUE_CREATED)
    public void onOrderCreated(@Payload OrderCreatedEvent event) {
        String trace = "orderId=" + event.orderId();
        String idemKey = "orders:processed:" + event.orderId();

        try {
            // 1. [데모용] 강제 결제 실패 시나리오 → 보상 트랜잭션 트리거
            if (props.getOrchestration().isFailPayment()) {
                log.warn("[Worker] Orchestration demo: forcing payment failure for {}", trace);
                triggerCompensation(event);
                return; // 멱등성 키 설정 없이 종료 (재처리/테스트 용이성)
            }

            // 2. 멱등성 체크 (Redis)
            if (redisTemplate.hasKey(idemKey)) {
                skippedCounter.increment();
                log.info("[Worker] Skip duplicated order event: {}", trace);
                return;
            }

            // 3. 실제 비즈니스 로직 처리 (Service 위임 권장 영역)
            processingTimer.record(() -> processOrderLogic(event, trace));

            // 4. 처리 완료 마킹 (TTL 설정)
            long ttlSec = Math.max(60, props.getIdempotency().getTtlSeconds());
            redisTemplate.opsForValue().set(idemKey, "1", Duration.ofSeconds(ttlSec));
            processedCounter.increment();

        } catch (TransientProcessingException e) {
            // 일시적 오류 → Retry Queue로 전송 (설정된 TTL 후 다시 메인 큐로 복귀)
            log.warn("[Worker] Transient error, send to retry: {} reason={}", trace, e.getMessage());
            rabbitTemplate.convertAndSend("", OrderAmqp.QUEUE_CREATED_RETRY, event);
        } catch (PermanentProcessingException e) {
            // 영구 실패 → 보상 트랜잭션(재고 복원)
            log.error("[Worker] Permanent error, triggering compensation for {}", trace, e);
            triggerCompensation(event);
        } catch (Exception e) {
            // 알 수 없는 오류 → 기본적으로 재시도 시도
            log.warn("[Worker] Unknown error, send to retry: {}", trace, e);
            rabbitTemplate.convertAndSend("", OrderAmqp.QUEUE_CREATED_RETRY, event);
        }
    }

    /**
     * 실제 비즈니스 로직 (추후 WorkerService 등으로 분리 권장)
     */
    private void processOrderLogic(OrderCreatedEvent event, String trace) {
        // 예: 외부 결제 승인 요청, 배송 시스템 요청 등
        log.info("[Worker] Consumed OrderCreatedEvent: {}, userId={}, productId={}, qty={}, total={}",
                trace, event.userId(), event.productId(), event.quantity(), event.totalPrice());
    }

    /**
     * 보상 트랜잭션 발행 (재고 복원 + 주문 취소)
     */
    private void triggerCompensation(OrderCreatedEvent event) {
        rabbitTemplate.convertAndSend(OrderAmqp.EXCHANGE,
                OrderAmqp.ROUTING_KEY_STOCK_RESTORE,
                new StockRestoreCommand(event.orderId(), event.productId(), event.quantity()));
    }

    // Custom Exceptions (별도 파일 분리 권장하지만, 편의상 내부 유지 시 static class)
    public static class TransientProcessingException extends RuntimeException {
        public TransientProcessingException(String message) { super(message); }
    }

    public static class PermanentProcessingException extends RuntimeException {
        public PermanentProcessingException(String message) { super(message); }
    }
}