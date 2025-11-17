package com.github.stella.springapiboard.board.repository;

import com.github.stella.springapiboard.board.dto.StatsDtos;

import java.util.List;

/**
 * 통계성 쿼리 전용 Repository (네이티브/복잡 쿼리 책임)
 */
public interface StatsQueryRepository {

    List<StatsDtos.DailyCount> findDailyPosts(int days);

    List<StatsDtos.TopItem> findTopCategories(int limit);

    List<StatsDtos.TopItem> findTopTags(int limit);

    List<StatsDtos.TopItem> findTopAuthors(int limit);
}
