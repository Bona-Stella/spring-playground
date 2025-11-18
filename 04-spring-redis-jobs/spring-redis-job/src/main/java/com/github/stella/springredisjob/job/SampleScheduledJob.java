package com.github.stella.springredisjob.job;

import com.github.stella.springredisjob.common.lock.RedisLockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class SampleScheduledJob {
    private static final Logger log = LoggerFactory.getLogger(SampleScheduledJob.class);
    private final RedisLockService lockService;

    public SampleScheduledJob(RedisLockService lockService) {
        this.lockService = lockService;
    }

    // 30초마다 실행, 분산락 확보 시에만 작업 실행
    @Scheduled(fixedDelay = 30_000, initialDelay = 5_000)
    public void rebuildCacheJob() {
        String lockKey = "lock:job:rebuild-cache";
        if (lockService.lock(lockKey, Duration.ofSeconds(10))) {
            try {
                log.info("[JOB] Rebuilding cache... (with redis lock)");
                // 실제 작업 로직 (예: 통계 집계/캐시 초기화 등)
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lockService.unlock(lockKey);
            }
        } else {
            log.debug("[JOB] Skip — lock held by another instance");
        }
    }
}
