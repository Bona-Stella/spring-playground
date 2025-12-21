package com.github.stella.springmsamq.worker.web;

import com.github.stella.springmsamq.common.event.OrderAmqp;
import com.github.stella.springmsamq.common.event.OrderCreatedEvent;
import com.github.stella.springmsamq.worker.config.WorkerProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DlqService {

    private final RabbitTemplate rabbitTemplate;
    private final AmqpAdmin amqpAdmin;
    private final WorkerProperties props;

    // DLQ 이름 상수화 (실제로는 Config나 공통 상수에서 가져오는 것이 좋음)
    private static final String DLQ_NAME = "orders.created.dlq";

    /**
     * DLQ 상태 조회 및 재처리 로직 수행
     */
    public Map<String, Object> processDlqMessages(boolean dryRun, Integer batchSize, String target) {
        // 1. 현재 DLQ 대기 건수 조회
        QueueInformation info = amqpAdmin.getQueueInfo(DLQ_NAME);
        int dlqCount = info != null ? info.getMessageCount() : 0;

        // 2. DryRun (단순 조회)
        if (dryRun) {
            return buildResult(dlqCount, 0, 0, target);
        }

        // 3. 메시지 이동 로직
        int limit = (batchSize != null && batchSize > 0) ? batchSize : Math.max(1, props.getReprocess().getBatchSize());
        int moved = 0;
        int failed = 0;

        for (int i = 0; i < limit; i++) {
            // receiveAndConvert는 Blocking 호출이므로 Timeout(1초) 설정
            Object obj = rabbitTemplate.receiveAndConvert(DLQ_NAME, 1000);
            if (obj == null) break;

            try {
                // 대상 Exchange/Queue 결정
                String routingKey = "main".equalsIgnoreCase(target) ? OrderAmqp.ROUTING_KEY_CREATED : OrderAmqp.QUEUE_CREATED_RETRY;
                String exchange = "main".equalsIgnoreCase(target) ? OrderAmqp.EXCHANGE : "";

                // 메시지 재발송
                if (obj instanceof OrderCreatedEvent event) {
                    rabbitTemplate.convertAndSend(exchange, routingKey, event);
                } else {
                    // 예상치 못한 객체라도 Retry 큐로 보냄
                    rabbitTemplate.convertAndSend("", OrderAmqp.QUEUE_CREATED_RETRY, obj);
                }
                moved++;
            } catch (Exception e) {
                log.error("[Worker] Failed to reprocess DLQ message", e);
                failed++;
            }
        }

        return buildResult(dlqCount, moved, failed, target);
    }

    private Map<String, Object> buildResult(int available, int moved, int failed, String target) {
        Map<String, Object> body = new HashMap<>();
        body.put("dlq", DLQ_NAME);
        body.put("available", available); // 처리 전 개수
        body.put("moved", moved);
        body.put("failed", failed);
        body.put("target", target);
        return body;
    }
}
