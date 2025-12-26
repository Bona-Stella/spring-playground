package com.github.stella.springapiboard.board.repository;

import com.github.stella.springapiboard.board.domain.Category;
import com.github.stella.springapiboard.board.domain.Post;
import com.github.stella.springapiboard.board.domain.Tag;
import com.github.stella.springapiboard.board.dto.StatsDtos;
import com.github.stella.springapiboard.board.repository.CategoryRepository;
import com.github.stella.springapiboard.board.repository.PostRepository;
import com.github.stella.springapiboard.board.repository.TagRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StatsQueryRepositoryIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    CategoryRepository categoryRepository;
    @Autowired
    TagRepository tagRepository;
    @Autowired
    PostRepository postRepository;
    @Autowired
    StatsQueryRepository statsQueryRepository; // @Primary -> Native impl
    @Autowired
    JPAQueryFactory jpaQueryFactory;

    StatsQueryRepositoryQuerydslImpl querydslImpl;

    @BeforeEach
    void setUp() {
        if (querydslImpl == null) {
            querydslImpl = new StatsQueryRepositoryQuerydslImpl(jpaQueryFactory);
        }

        // Clean up tables (order matters due to FKs)
        postRepository.deleteAll();
        tagRepository.deleteAll();
        categoryRepository.deleteAll();

        // Seed sample data
        Category catJava = categoryRepository.save(new Category("Java", "java", "Java posts"));
        Category catSpring = categoryRepository.save(new Category("Spring", "spring", "Spring posts"));

        Tag tagJPA = tagRepository.save(new Tag("jpa", "JPA"));
        Tag tagBoot = tagRepository.save(new Tag("spring-boot", "Spring Boot"));

        // Post 1: Java + JPA
        Post p1 = new Post("Post1", "content1", "alice");
        p1.changeCategory(catJava);
        p1.addTag(tagJPA);
        postRepository.save(p1);

        // Post 2: Spring + Boot
        Post p2 = new Post("Post2", "content2", "bob");
        p2.changeCategory(catSpring);
        p2.addTag(tagBoot);
        postRepository.save(p2);

        // Post 3: Spring + Boot
        Post p3 = new Post("Post3", "content3", "bob");
        p3.changeCategory(catSpring);
        p3.addTag(tagBoot);
        postRepository.save(p3);
    }

    @Test
    @DisplayName("Native and Querydsl results should be consistent for top categories")
    void topCategories_consistency() {
        List<StatsDtos.TopItem> nativeTop = statsQueryRepository.findTopCategories(5);
        List<StatsDtos.TopItem> dslTop = querydslImpl.findTopCategories(5);

        assertThat(nativeTop).isNotEmpty();
        assertThat(dslTop).isNotEmpty();
        assertThat(dslTop.get(0).name()).isEqualTo("Spring");
        assertThat(nativeTop.get(0).name()).isEqualTo("Spring");
        assertThat(dslTop.get(0).count()).isEqualTo(2);
        assertThat(nativeTop.get(0).count()).isEqualTo(2);
    }

    @Test
    @DisplayName("Native and Querydsl results should be consistent for top tags")
    void topTags_consistency() {
        List<StatsDtos.TopItem> nativeTop = statsQueryRepository.findTopTags(5);
        List<StatsDtos.TopItem> dslTop = querydslImpl.findTopTags(5);

        assertThat(nativeTop).isNotEmpty();
        assertThat(dslTop).isNotEmpty();
        assertThat(dslTop.get(0).name()).isEqualTo("Spring Boot");
        assertThat(nativeTop.get(0).name()).isEqualTo("Spring Boot");
        assertThat(dslTop.get(0).count()).isEqualTo(2);
        assertThat(nativeTop.get(0).count()).isEqualTo(2);
    }

    @Test
    @DisplayName("Top authors should include counts grouped by author")
    void topAuthors_basic() {
        List<StatsDtos.TopItem> items = statsQueryRepository.findTopAuthors(5);
        assertThat(items).isNotEmpty();
        assertThat(items.get(0).name()).isEqualTo("bob");
        assertThat(items.get(0).count()).isEqualTo(2);
    }
}
