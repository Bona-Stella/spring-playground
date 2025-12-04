package com.github.stella.springmsamq.worker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "worker")
public class WorkerProperties {
    private final Vthreads vthreads = new Vthreads();
    private final Rabbit rabbit = new Rabbit();
    private final Retry retry = new Retry();
    private final Idempotency idempotency = new Idempotency();
    private final Reprocess reprocess = new Reprocess();
    private final Orchestration orchestration = new Orchestration();

    public Vthreads getVthreads() { return vthreads; }
    public Rabbit getRabbit() { return rabbit; }
    public Retry getRetry() { return retry; }
    public Idempotency getIdempotency() { return idempotency; }
    public Reprocess getReprocess() { return reprocess; }
    public Orchestration getOrchestration() { return orchestration; }

    public static class Vthreads {
        private boolean enabled = true;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    public static class Rabbit {
        private int concurrency = 2;
        public int getConcurrency() { return concurrency; }
        public void setConcurrency(int concurrency) { this.concurrency = concurrency; }
    }

    public static class Retry {
        private long ttl = 5000L; // milliseconds
        public long getTtl() { return ttl; }
        public void setTtl(long ttl) { this.ttl = ttl; }
    }

    public static class Idempotency {
        private long ttlSeconds = 86400; // 24h
        public long getTtlSeconds() { return ttlSeconds; }
        public void setTtlSeconds(long ttlSeconds) { this.ttlSeconds = ttlSeconds; }
    }

    public static class Reprocess {
        private int batchSize = 100;
        private String token = "dev-token"; // 운영 환경에서는 외부화/비밀관리 사용
        public int getBatchSize() { return batchSize; }
        public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
    }

    public static class Orchestration {
        /** 데모/검증용: 결제 실패를 강제하여 보상 트랜잭션을 트리거 */
        private boolean failPayment = false;
        public boolean isFailPayment() { return failPayment; }
        public void setFailPayment(boolean failPayment) { this.failPayment = failPayment; }
    }
}
