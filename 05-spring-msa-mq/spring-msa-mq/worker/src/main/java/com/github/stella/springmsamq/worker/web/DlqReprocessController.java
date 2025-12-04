package com.github.stella.springmsamq.worker.web;

import com.github.stella.springmsamq.common.ApiResponse;
import com.github.stella.springmsamq.common.event.OrderAmqp;
import com.github.stella.springmsamq.common.event.OrderCreatedEvent;
import com.github.stella.springmsamq.worker.config.WorkerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/worker/dlq")
public class DlqReprocessController {
    private static final Logger log = LoggerFactory.getLogger(DlqReprocessController.class);

    private final RabbitTemplate rabbitTemplate;
    private final AmqpAdmin amqpAdmin;
    private final WorkerProperties props;

    public DlqReprocessController(RabbitTemplate rabbitTemplate,
                                  AmqpAdmin amqpAdmin,
                                  WorkerProperties props) {
        this.rabbitTemplate = rabbitTemplate;
        this.amqpAdmin = amqpAdmin;
        this.props = props;
    }

    /**
     * DLQ 메시지를 재처리(retry/main)로 이동시키는 운영자 전용 엔드포인트
     *
     * Params:
     * - token: 보호 토큰(필수)
     * - dryRun: true면 소비/이동 없이 DLQ 큐의 메시지 수만 반환
     * - batchSize: 한 번에 이동할 최대 건수(기본 설정값 사용)
     * - target: retry | main (기본 retry)
     */
    @PostMapping(value = "/reprocess", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<Map<String, Object>> reprocess(@RequestParam(name = "token") String token,
                                                      @RequestParam(name = "dryRun", defaultValue = "false") boolean dryRun,
                                                      @RequestParam(name = "batchSize", required = false) Integer batchSize,
                                                      @RequestParam(name = "target", defaultValue = "retry") String target,
                                                      HttpServletRequest request) {
        if (token == null || !token.equals(props.getReprocess().getToken())) {
            return ApiResponse.of(401, "UNAUTHORIZED", "Invalid token", null, request.getRequestURI());
        }

        String dlqName = "orders.created.dlq"; // DLQ 이름 (Worker RabbitConfig 참조)

        QueueInformation info = amqpAdmin.getQueueInfo(dlqName);
        int dlqCount = info != null ? info.getMessageCount() : 0;

        if (dryRun) {
            Map<String, Object> body = new HashMap<>();
            body.put("dlq", dlqName);
            body.put("available", dlqCount);
            body.put("moved", 0);
            body.put("failed", 0);
            return ApiResponse.success(body, request.getRequestURI());
        }

        int limit = (batchSize != null && batchSize > 0) ? batchSize : Math.max(1, props.getReprocess().getBatchSize());
        int moved = 0;
        int failed = 0;

        for (int i = 0; i < limit; i++) {
            Object obj = rabbitTemplate.receiveAndConvert(dlqName, 1000); // 1s timeout
            if (obj == null) {
                break; // 더 이상 메시지 없음
            }
            try {
                if (obj instanceof OrderCreatedEvent event) {
                    if ("main".equalsIgnoreCase(target)) {
                        rabbitTemplate.convertAndSend(OrderAmqp.EXCHANGE, OrderAmqp.ROUTING_KEY_CREATED, event);
                    } else {
                        // 기본 retry
                        rabbitTemplate.convertAndSend("", OrderAmqp.QUEUE_CREATED_RETRY, event);
                    }
                    moved++;
                } else {
                    // 예상치 못한 페이로드는 그대로 폐기하지 않고 재시도 큐로 이동
                    rabbitTemplate.convertAndSend("", OrderAmqp.QUEUE_CREATED_RETRY, obj);
                    moved++;
                }
            } catch (Exception e) {
                log.error("[Worker] Failed to reprocess DLQ message", e);
                failed++;
            }
        }

        Map<String, Object> body = new HashMap<>();
        body.put("dlq", dlqName);
        body.put("available", dlqCount);
        body.put("moved", moved);
        body.put("failed", failed);
        body.put("target", target);
        return ApiResponse.success(body, request.getRequestURI());
    }
}
