package com.github.stella.springapiboard.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class StatsCacheEvictor {

    private static final Logger log = LoggerFactory.getLogger(StatsCacheEvictor.class);

    // 매일 새벽 3시에 통계 캐시 전체 비우기
    @Scheduled(cron = "0 0 3 * * *")
    @CacheEvict(cacheNames = {"statsDailyPosts", "statsTopCategories", "statsTopTags", "statsTopAuthors"}, allEntries = true)
    public void evictStatsCaches() {
        log.info("Evicted all stats caches");
    }
}
