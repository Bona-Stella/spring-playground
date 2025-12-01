package com.github.stella.springredisjob.job;

import com.github.stella.springredisjob.common.lock.annotation.DistributedLockable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SampleScheduledJob {
    private static final Logger log = LoggerFactory.getLogger(SampleScheduledJob.class);

    // 30초마다 실행, 분산락 확보 시에만 작업 실행
    @Scheduled(fixedDelay = 30_000, initialDelay = 5_000)
    @DistributedLockable(key = "'job:rebuild-cache'", ttlSeconds = 10, onFail = DistributedLockable.OnFail.SKIP)
    public void rebuildCacheJob() {
        log.info("[JOB] Rebuilding cache... (with annotation lock)");
        // 실제 작업 로직 (예: 통계 집계/캐시 초기화 등)
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
