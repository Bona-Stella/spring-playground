package com.github.stella.springapiboard.board.repository;

import com.github.stella.springapiboard.board.dto.StatsDtos;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
public class StatsQueryRepositoryImpl implements StatsQueryRepository {

    private final EntityManager em;

    public StatsQueryRepositoryImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    public List<StatsDtos.DailyCount> findDailyPosts(int days) {
        String sql = """
                select cast(p.created_at as date) as d, count(*) as c
                from posts p
                where p.created_at >= current_date - make_interval(days => :days)
                group by d
                order by d asc
                """;
        Query q = em.createNativeQuery(sql);
        q.setParameter("days", days);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        List<StatsDtos.DailyCount> result = new ArrayList<>();
        for (Object[] r : rows) {
            LocalDate day = ((java.sql.Date) r[0]).toLocalDate();
            long count = ((Number) r[1]).longValue();
            result.add(new StatsDtos.DailyCount(day, count));
        }
        return result;
    }

    @Override
    public List<StatsDtos.TopItem> findTopCategories(int limit) {
        String sql = """
                select c.id, c.name, count(p.id) as cnt
                from categories c
                left join posts p on p.category_id = c.id
                group by c.id, c.name
                order by cnt desc
                limit :limit
                """;
        Query q = em.createNativeQuery(sql);
        q.setParameter("limit", limit);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        List<StatsDtos.TopItem> result = new ArrayList<>();
        for (Object[] r : rows) {
            Long id = r[0] == null ? null : ((Number) r[0]).longValue();
            String name = (String) r[1];
            long cnt = ((Number) r[2]).longValue();
            result.add(new StatsDtos.TopItem(id, name, cnt));
        }
        return result;
    }

    @Override
    public List<StatsDtos.TopItem> findTopTags(int limit) {
        String sql = """
                select t.id, t.name, count(pt.post_id) as cnt
                from tags t
                left join post_tags pt on pt.tag_id = t.id
                group by t.id, t.name
                order by cnt desc
                limit :limit
                """;
        Query q = em.createNativeQuery(sql);
        q.setParameter("limit", limit);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        List<StatsDtos.TopItem> result = new ArrayList<>();
        for (Object[] r : rows) {
            Long id = r[0] == null ? null : ((Number) r[0]).longValue();
            String name = (String) r[1];
            long cnt = ((Number) r[2]).longValue();
            result.add(new StatsDtos.TopItem(id, name, cnt));
        }
        return result;
    }

    @Override
    public List<StatsDtos.TopItem> findTopAuthors(int limit) {
        String sql = """
                select null as id, p.author as name, count(*) as cnt
                from posts p
                group by p.author
                order by cnt desc
                limit :limit
                """;
        Query q = em.createNativeQuery(sql);
        q.setParameter("limit", limit);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        List<StatsDtos.TopItem> result = new ArrayList<>();
        for (Object[] r : rows) {
            Long id = null;
            String name = (String) r[1];
            long cnt = ((Number) r[2]).longValue();
            result.add(new StatsDtos.TopItem(id, name, cnt));
        }
        return result;
    }
}
