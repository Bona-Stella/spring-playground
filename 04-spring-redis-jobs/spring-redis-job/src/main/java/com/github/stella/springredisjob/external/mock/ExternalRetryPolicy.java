package com.github.stella.springredisjob.external.mock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

@Component
public class ExternalRetryPolicy {
    private static final Logger log = LoggerFactory.getLogger(ExternalRetryPolicy.class);

    @Value("${external.mock.maxAttempts:3}")
    private int maxAttempts;
    @Value("${external.mock.baseDelayMs:100}")
    private long baseDelayMs;
    @Value("${external.mock.jitterPct:0.3}")
    private double jitterPct;

    public <T> T executeWithRetry(Supplier<T> supplier) throws Exception {
        int attempt = 0;
        Exception last = null;
        while (attempt < Math.max(1, maxAttempts)) {
            try {
                return supplier.get();
            } catch (Exception e) {
                last = e;
                attempt++;
                if (attempt >= maxAttempts) break;
                long backoff = (long) (baseDelayMs * Math.pow(2, attempt - 1));
                double jitterFactor = 1 + (ThreadLocalRandom.current().nextDouble() * 2 - 1) * jitterPct; // 1±pct
                long sleep = Math.max(10, (long) (backoff * jitterFactor));
                log.debug("[Retry] attempt {}/{} failed: {} → sleep {}ms", attempt, maxAttempts, e.getMessage(), sleep);
                try { Thread.sleep(sleep); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); throw ie; }
            }
        }
        throw last != null ? last : new RuntimeException("Unknown failure in retry");
    }
}
