package com.github.stella.springmsamq.worker.listener;

import com.github.stella.springmsamq.common.event.OrderAmqp;
import com.github.stella.springmsamq.common.event.StockRestoreCommand;
import com.github.stella.springmsamq.common.event.OrderCreatedEvent;
import com.github.stella.springmsamq.worker.config.WorkerProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class OrderCreatedListener {
    private static final Logger log = LoggerFactory.getLogger(OrderCreatedListener.class);

    private final RabbitTemplate rabbitTemplate;
    private final StringRedisTemplate redisTemplate;
    private final WorkerProperties props;
    private final Counter processedCounter;
    private final Counter skippedCounter;
    private final Timer processingTimer;

    public OrderCreatedListener(RabbitTemplate rabbitTemplate,
                                StringRedisTemplate redisTemplate,
                                WorkerProperties props,
                                MeterRegistry meterRegistry) {
        this.rabbitTemplate = rabbitTemplate;
        this.redisTemplate = redisTemplate;
        this.props = props;
        this.processedCounter = meterRegistry.counter("worker.orders.processed");
        this.skippedCounter = meterRegistry.counter("worker.orders.skipped.idempotent");
        this.processingTimer = meterRegistry.timer("worker.orders.processing.time");
    }

    @RabbitListener(queues = OrderAmqp.QUEUE_CREATED)
    public void onOrderCreated(@Payload OrderCreatedEvent event) {
        String trace = "orderId=" + event.orderId();
        String idemKey = "orders:processed:" + event.orderId();
        try {
            // 오케스트레이션 데모: 강제 결제 실패 시나리오 → 보상 트랜잭션(재고 복원) 트리거
            if (props.getOrchestration().isFailPayment()) {
                log.warn("[Worker] Orchestration demo: forcing payment failure for {}", trace);
                // 보상 커맨드 발행 (재고 복원 + 주문 취소)
                rabbitTemplate.convertAndSend(OrderAmqp.EXCHANGE,
                        OrderAmqp.ROUTING_KEY_STOCK_RESTORE,
                        new StockRestoreCommand(event.orderId(), event.productId(), event.quantity()));
                // 멱등성 키를 설정하지 않고 종료해 재처리 여지를 남김(데모 목적)
                return;
            }
            // 멱등성 체크
            Boolean exists = redisTemplate.hasKey(idemKey);
            if (Boolean.TRUE.equals(exists)) {
                skippedCounter.increment();
                log.info("[Worker] Skip duplicated order event due to idempotency: {}", trace);
                return;
            }

            processingTimer.record(() -> {
                // 실제 처리 로직 (예: 결제/알림) — 데모로 로그
                log.info("[Worker] Consumed OrderCreatedEvent: {}, userId={}, productId={}, qty={}, total={}",
                        trace, event.userId(), event.productId(), event.quantity(), event.totalPrice());
            });

            // 처리 완료 후 멱등성 키 설정
            long ttlSec = Math.max(60, props.getIdempotency().getTtlSeconds());
            redisTemplate.opsForValue().set(idemKey, "1", Duration.ofSeconds(ttlSec));
            processedCounter.increment();
        } catch (TransientProcessingException e) {
            // 일시적 오류 → Retry 큐로 이동 (TTL 후 메인으로 재유입)
            log.warn("[Worker] Transient error, send to retry: {} reason={}", trace, e.getMessage());
            rabbitTemplate.convertAndSend("", OrderAmqp.QUEUE_CREATED_RETRY, event);
        } catch (PermanentProcessingException e) {
            // 영구 실패 → 보상 트랜잭션 트리거(재고 복원 커맨드 발행)
            log.error("[Worker] Permanent error, triggering compensation for {}", trace, e);
            rabbitTemplate.convertAndSend(OrderAmqp.EXCHANGE,
                    OrderAmqp.ROUTING_KEY_STOCK_RESTORE,
                    new StockRestoreCommand(event.orderId(), event.productId(), event.quantity()));
            // DLQ로도 보내고 싶다면 rethrow, 그렇지 않으면 종료
            return;
        } catch (Exception e) {
            // 기본은 재시도 가능으로 간주
            log.warn("[Worker] Unknown error, send to retry: {}", trace, e);
            rabbitTemplate.convertAndSend("", OrderAmqp.QUEUE_CREATED_RETRY, event);
        }
    }

    public static class TransientProcessingException extends RuntimeException {
        public TransientProcessingException(String message) { super(message); }
    }

    public static class PermanentProcessingException extends RuntimeException {
        public PermanentProcessingException(String message) { super(message); }
    }
}
