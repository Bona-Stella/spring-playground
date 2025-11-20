package com.github.stella.springapiboard.board.repository;

import com.github.stella.springapiboard.board.domain.QCategory;
import com.github.stella.springapiboard.board.domain.QPost;
import com.github.stella.springapiboard.board.domain.QTag;
import com.github.stella.springapiboard.board.dto.StatsDtos;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * StatsQueryRepository의 QueryDSL 기반 구현
 */
@Repository
public class StatsQueryRepositoryQuerydslImpl implements StatsQueryRepository {

    private final JPAQueryFactory queryFactory;

    public StatsQueryRepositoryQuerydslImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public List<StatsDtos.DailyCount> findDailyPosts(int days) {
        QPost p = QPost.post;
        LocalDateTime start = LocalDate.now().minusDays(days).atStartOfDay();

        var dayExpr = Expressions.dateTemplate(LocalDate.class, "cast({0} as date)", p.createdAt);

        List<Tuple> rows = queryFactory
                .select(dayExpr, p.id.count())
                .from(p)
                .where(p.createdAt.goe(start))
                .groupBy(dayExpr)
                .orderBy(dayExpr.asc())
                .fetch();

        List<StatsDtos.DailyCount> result = new ArrayList<>();
        for (Tuple t : rows) {
            LocalDate day = t.get(dayExpr);
            long cnt = t.get(p.id.count());
            result.add(new StatsDtos.DailyCount(day, cnt));
        }
        return result;
    }

    @Override
    public List<StatsDtos.TopItem> findTopCategories(int limit) {
        QPost p = QPost.post;
        QCategory c = QCategory.category;

        List<Tuple> rows = queryFactory
                .select(c.id, c.name, p.id.count())
                .from(c)
                .leftJoin(p).on(p.category.id.eq(c.id))
                .groupBy(c.id, c.name)
                .orderBy(p.id.count().desc())
                .limit(limit)
                .fetch();

        List<StatsDtos.TopItem> result = new ArrayList<>();
        for (Tuple t : rows) {
            Long id = t.get(c.id);
            String name = t.get(c.name);
            long cnt = t.get(p.id.count());
            result.add(new StatsDtos.TopItem(id, name, cnt));
        }
        return result;
    }

    @Override
    public List<StatsDtos.TopItem> findTopTags(int limit) {
        QTag t = QTag.tag;
        QPost p = QPost.post;
        QTag t2 = new QTag("t2");

        var postCountSub = JPAExpressions
                .select(p.id.count())
                .from(p)
                .join(p.tags, t2)
                .where(t2.id.eq(t.id));

        NumberExpression<Long> cntExpr = Expressions.numberTemplate(Long.class, "({0})", postCountSub);

        List<Tuple> rows = queryFactory
                .select(t.id, t.name, cntExpr)
                .from(t)
                .orderBy(cntExpr.desc())
                .limit(limit)
                .fetch();

        List<StatsDtos.TopItem> result = new ArrayList<>();
        for (Tuple row : rows) {
            Long id = row.get(t.id);
            String name = row.get(t.name);
            long cnt = row.get(cntExpr);
            result.add(new StatsDtos.TopItem(id, name, cnt));
        }
        return result;
    }

    @Override
    public List<StatsDtos.TopItem> findTopAuthors(int limit) {
        QPost p = QPost.post;

        List<Tuple> rows = queryFactory
                .select(p.author, p.id.count())
                .from(p)
                .groupBy(p.author)
                .orderBy(p.id.count().desc())
                .limit(limit)
                .fetch();

        List<StatsDtos.TopItem> result = new ArrayList<>();
        for (Tuple row : rows) {
            String name = row.get(p.author);
            long cnt = row.get(p.id.count());
            result.add(new StatsDtos.TopItem(null, name, cnt));
        }
        return result;
    }
}
