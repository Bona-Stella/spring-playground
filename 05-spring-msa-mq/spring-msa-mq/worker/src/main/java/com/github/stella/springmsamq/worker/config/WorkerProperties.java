package com.github.stella.springmsamq.worker.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "worker")
public class WorkerProperties {
    private final Vthreads vthreads = new Vthreads();
    private final Rabbit rabbit = new Rabbit();
    private final Retry retry = new Retry();
    private final Idempotency idempotency = new Idempotency();
    private final Reprocess reprocess = new Reprocess();
    private final Orchestration orchestration = new Orchestration();

    @Data
    public static class Vthreads {
        private boolean enabled = true;
    }

    @Data
    public static class Rabbit {
        private int concurrency = 2;
    }

    @Data
    public static class Retry {
        private long ttl = 5000L; // 5 seconds
    }

    @Data
    public static class Idempotency {
        private long ttlSeconds = 86400; // 24h
    }

    @Data
    public static class Reprocess {
        private int batchSize = 100;
        private String token = "dev-token";
    }

    @Data
    public static class Orchestration {
        /** 데모/검증용: 결제 실패 시뮬레이션 */
        private boolean failPayment = false;
    }
}
