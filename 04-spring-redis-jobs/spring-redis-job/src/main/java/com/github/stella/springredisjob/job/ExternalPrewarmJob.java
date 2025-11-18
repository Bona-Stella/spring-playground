package com.github.stella.springredisjob.job;

import com.github.stella.springredisjob.common.lock.RedisLockService;
import com.github.stella.springredisjob.external.mock.ExternalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
public class ExternalPrewarmJob {
    private static final Logger log = LoggerFactory.getLogger(ExternalPrewarmJob.class);
    private final RedisLockService lockService;
    private final ExternalService externalService;

    private static final List<String> HOT_CITIES = List.of("seoul", "busan", "tokyo");

    public ExternalPrewarmJob(RedisLockService lockService, ExternalService externalService) {
        this.lockService = lockService;
        this.externalService = externalService;
    }

    // 2분 주기로 캐시 프리워밍 (분산락 보장)
    @Scheduled(fixedDelay = 120_000, initialDelay = 15_000)
    public void prewarm() {
        String lockKey = "lock:job:external:prewarm";
        if (lockService.lock(lockKey, Duration.ofSeconds(20))) {
            try {
                for (String city : HOT_CITIES) {
                    try {
                        externalService.getWeatherCached(city);
                        log.info("[Prewarm] warmed city={}", city);
                    } catch (Exception e) {
                        log.warn("[Prewarm] failed for city={} err={}", city, e.getMessage());
                    }
                }
            } finally {
                lockService.unlock(lockKey);
            }
        } else {
            log.debug("[Prewarm] skipped — lock held by another instance");
        }
    }
}
