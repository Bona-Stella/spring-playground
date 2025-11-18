package com.github.stella.springapiboard.board.repository;

import com.github.stella.springapiboard.board.domain.Post;
import com.github.stella.springapiboard.board.dto.PostSearchDtos.PostSearchCondition;
import com.github.stella.springapiboard.board.domain.QCategory;
import com.github.stella.springapiboard.board.domain.QPost;
import com.github.stella.springapiboard.board.domain.QTag;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class PostQueryRepositoryImpl implements PostQueryRepository {

    private final JPAQueryFactory queryFactory;

    public PostQueryRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public Page<Post> search(PostSearchCondition condition, Pageable pageable) {
        QPost p = QPost.post;
        QCategory c = QCategory.category;
        QTag t = QTag.tag;

        JPQLQuery<Post> base = queryFactory
                .selectFrom(p)
                .leftJoin(p.category, c).fetchJoin()
                .leftJoin(p.tags, t).fetchJoin()
                .where(
                        keywordContains(condition.keyword()),
                        authorEq(condition.author()),
                        categoryEq(condition.categoryId()),
                        createdBetween(condition.from(), condition.to())
                )
                .distinct();

        // 태그 AND 조건: 지정된 tagIds를 모두 가진 게시글만
        if (condition.tagIds() != null && !condition.tagIds().isEmpty()) {
            base = base.where(t.id.in(condition.tagIds()))
                    .groupBy(p.id)
                    .having(t.id.countDistinct().eq((long) condition.tagIds().size()));
        }

        // count 쿼리 (fetchJoin 제거, groupBy 조건 유지 필요 시 동일 having 적용)
        JPQLQuery<Long> countQuery = queryFactory
                .select(p.id.countDistinct())
                .from(p)
                .leftJoin(p.tags, t)
                .where(
                        keywordContains(condition.keyword()),
                        authorEq(condition.author()),
                        categoryEq(condition.categoryId()),
                        createdBetween(condition.from(), condition.to())
                );
        if (condition.tagIds() != null && !condition.tagIds().isEmpty()) {
            countQuery = countQuery.where(t.id.in(condition.tagIds()))
                    .groupBy(p.id)
                    .having(t.id.countDistinct().eq((long) condition.tagIds().size()));
            // countDistinct over grouped set — wrap with select count(*) from (subquery) 형태가 아니므로
            // 간단히 사이즈 재계산을 위해 실행 후 리스트 크기 사용
            List<Long> ids = queryFactory
                    .select(p.id)
                    .from(p)
                    .leftJoin(p.tags, t)
                    .where(
                            keywordContains(condition.keyword()),
                            authorEq(condition.author()),
                            categoryEq(condition.categoryId()),
                            createdBetween(condition.from(), condition.to()),
                            t.id.in(condition.tagIds())
                    )
                    .groupBy(p.id)
                    .having(t.id.countDistinct().eq((long) condition.tagIds().size()))
                    .fetch();
            long total = ids.size();
            List<Post> content = base
                    .orderBy(p.id.desc())
                    .offset(pageable.getOffset())
                    .limit(pageable.getPageSize())
                    .fetch();
            return new PageImpl<>(content, pageable, total);
        }

        long total = countQuery.fetchFirst() == null ? 0L : countQuery.fetchFirst();
        List<Post> content = base
                .orderBy(p.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
        return new PageImpl<>(content, pageable, total);
    }

    private BooleanExpression keywordContains(String keyword) {
        if (keyword == null || keyword.isBlank()) return null;
        QPost p = QPost.post;
        return p.title.containsIgnoreCase(keyword).or(p.content.containsIgnoreCase(keyword));
    }

    private BooleanExpression authorEq(String author) {
        if (author == null || author.isBlank()) return null;
        QPost p = QPost.post;
        return p.author.eq(author);
    }

    private BooleanExpression categoryEq(Long categoryId) {
        if (categoryId == null) return null;
        QPost p = QPost.post;
        return p.category.id.eq(categoryId);
    }

    private BooleanExpression createdBetween(java.time.LocalDate from, java.time.LocalDate to) {
        QPost p = QPost.post;
        if (from == null && to == null) return null;
        LocalDateTime start = from == null ? LocalDateTime.MIN : from.atStartOfDay();
        LocalDateTime end = to == null ? LocalDateTime.MAX : to.plusDays(1).atStartOfDay();
        return p.createdAt.goe(start).and(p.createdAt.lt(end));
    }
}
