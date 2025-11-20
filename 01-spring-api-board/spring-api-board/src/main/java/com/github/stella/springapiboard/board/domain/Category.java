package com.github.stella.springapiboard.board.domain;

import com.github.stella.springapiboard.common.jpa.BaseTimeEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "categories", indexes = {
        // name = 인덱스 이름 설정, columeList = 인덱스 설정 컬럼
        @Index(name = "idx_category_slug", columnList = "slug", unique = true)
})
public class Category extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 120, unique = true)
    private String slug;

    @Column(length = 255)
    private String description;

    // Jpa는 반드시 생성자를 public or protected로 선언해야 하며 가능하면 protected로 보호한다.
    // Lombok -> @NoArgsConstructor(access = AccessLevel.PROTECTED) 도 같은 기능
    protected Category() {}

    public Category(String name, String slug, String description) {
        this.name = name;
        this.slug = slug;
        this.description = description;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public String getDescription() { return description; }

    public void update(String name, String slug, String description) {
        if (name != null) this.name = name;
        if (slug != null) this.slug = slug;
        if (description != null) this.description = description;
    }
}
