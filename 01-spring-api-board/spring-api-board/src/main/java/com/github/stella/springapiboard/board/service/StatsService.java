package com.github.stella.springapiboard.board.service;

import com.github.stella.springapiboard.board.dto.StatsDtos;
import com.github.stella.springapiboard.board.repository.StatsQueryRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class StatsService {

    private final StatsQueryRepository statsQueryRepository;

    public StatsService(StatsQueryRepository statsQueryRepository) {
        this.statsQueryRepository = statsQueryRepository;
    }

    /**
     * 최근 N일 동안 일별 게시글 수
     */
    @Cacheable(cacheNames = "statsDailyPosts", key = "#days")
    public List<StatsDtos.DailyCount> dailyPosts(int days) {
        if (days <= 0) days = 30;
        return statsQueryRepository.findDailyPosts(days);
    }

    /**
     * 카테고리별 게시글 수 Top N
     */
    @Cacheable(cacheNames = "statsTopCategories", key = "#limit")
    public List<StatsDtos.TopItem> topCategories(int limit) {
        if (limit <= 0) limit = 5;
        return statsQueryRepository.findTopCategories(limit);
    }

    /**
     * 태그별 사용 빈도 Top N
     */
    @Cacheable(cacheNames = "statsTopTags", key = "#limit")
    public List<StatsDtos.TopItem> topTags(int limit) {
        if (limit <= 0) limit = 5;
        return statsQueryRepository.findTopTags(limit);
    }

    /**
     * 작성자별 게시글 수 Top N
     */
    @Cacheable(cacheNames = "statsTopAuthors", key = "#limit")
    public List<StatsDtos.TopItem> topAuthors(int limit) {
        if (limit <= 0) limit = 5;
        return statsQueryRepository.findTopAuthors(limit);
    }
}
