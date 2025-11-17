package com.github.stella.springapiboard.board.domain;

import com.github.stella.springapiboard.common.jpa.BaseTimeEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "posts")
public class Post extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Lob
    @Column(nullable = false)
    private String content;

    @Column(nullable = false, length = 100)
    private String author;

    protected Post() {}

    public Post(String title, String content, String author) {
        this.title = title;
        this.content = content;
        this.author = author;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getAuthor() { return author; }

    public void update(String title, String content) {
        if (title != null) this.title = title;
        if (content != null) this.content = content;
    }
}
