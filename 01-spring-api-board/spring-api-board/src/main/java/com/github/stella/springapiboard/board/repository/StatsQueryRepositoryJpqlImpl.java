package com.github.stella.springapiboard.board.repository;

import com.github.stella.springapiboard.board.dto.StatsDtos;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * StatsQueryRepository의 JPQL 기반 구현
 * 결과는 네이티브/QueryDSL 구현과 동일하게 반환하도록 매핑한다.
 */
@Repository
public class StatsQueryRepositoryJpqlImpl implements StatsQueryRepository {

    private final EntityManager em;

    public StatsQueryRepositoryJpqlImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    public List<StatsDtos.DailyCount> findDailyPosts(int days) {
        LocalDateTime start = LocalDate.now().minusDays(days).atStartOfDay();
        String jpql = """
                select function('date', p.createdAt) as d, count(p.id) as c
                from Post p
                where p.createdAt >= :start
                group by function('date', p.createdAt)
                order by function('date', p.createdAt) asc
                """;
        @SuppressWarnings("unchecked")
        TypedQuery<Object[]> q = (TypedQuery<Object[]>) (TypedQuery<?>) em.createQuery(jpql);
        q.setParameter("start", start);
        List<Object[]> rows = q.getResultList();
        List<StatsDtos.DailyCount> result = new ArrayList<>();
        for (Object[] r : rows) {
            LocalDate day = (r[0] instanceof java.sql.Date sd) ? sd.toLocalDate() : (LocalDate) r[0];
            long count = ((Number) r[1]).longValue();
            result.add(new StatsDtos.DailyCount(day, count));
        }
        return result;
    }

    @Override
    public List<StatsDtos.TopItem> findTopCategories(int limit) {
        String jpql = """
                select c.id, c.name, count(p.id)
                from Category c
                left join Post p on p.category = c
                group by c.id, c.name
                order by count(p.id) desc
                """;
        @SuppressWarnings("unchecked")
        TypedQuery<Object[]> q = (TypedQuery<Object[]>) (TypedQuery<?>) em.createQuery(jpql);
        q.setMaxResults(Math.max(0, limit));
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
        // 태그별 포스트 수를 서브쿼리로 계산하여 0건 태그도 포함한다
        String jpql = """
                select t.id, t.name,
                       (select count(p2.id) from Post p2 where t member of p2.tags) as cnt
                from Tag t
                order by (select count(p3.id) from Post p3 where t member of p3.tags) desc
                """;
        @SuppressWarnings("unchecked")
        TypedQuery<Object[]> q = (TypedQuery<Object[]>) (TypedQuery<?>) em.createQuery(
                "select t.id, t.name, (select count(p2.id) from Post p2 where t member of p2.tags) as cnt " +
                "from Tag t order by (select count(p3.id) from Post p3 where t member of p3.tags) desc"
        );
        q.setMaxResults(Math.max(0, limit));
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
        String jpql = """
                select null, p.author, count(p.id)
                from Post p
                group by p.author
                order by count(p.id) desc
                """;
        @SuppressWarnings("unchecked")
        TypedQuery<Object[]> q = (TypedQuery<Object[]>) (TypedQuery<?>) em.createQuery(jpql);
        q.setMaxResults(Math.max(0, limit));
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
