package com.github.stella.springredisjob.job;

import com.github.stella.springredisjob.common.lock.annotation.DistributedLockable;
import com.github.stella.springredisjob.external.mock.ExternalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ExternalPrewarmJob {
    private static final Logger log = LoggerFactory.getLogger(ExternalPrewarmJob.class);
    private final ExternalService externalService;

    private static final List<String> HOT_CITIES = List.of("seoul", "busan", "tokyo");

    public ExternalPrewarmJob(ExternalService externalService) {
        this.externalService = externalService;
    }

    // 2분 주기로 캐시 프리워밍 (분산락 보장)
    @Scheduled(fixedDelay = 120_000, initialDelay = 15_000)
    @DistributedLockable(key = "'job:external:prewarm'", ttlSeconds = 20, onFail = DistributedLockable.OnFail.SKIP)
    public void prewarm() {
        for (String city : HOT_CITIES) {
            try {
                externalService.getWeatherCached(city);
                log.info("[Prewarm] warmed city={}", city);
            } catch (Exception e) {
                log.warn("[Prewarm] failed for city={} err={}", city, e.getMessage());
            }
        }
    }
}
