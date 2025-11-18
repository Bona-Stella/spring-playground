package com.github.stella.springapiboard.board.domain;

import com.github.stella.springapiboard.common.jpa.BaseTimeEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "file_attachments", indexes = {
        @Index(name = "idx_file_post", columnList = "post_id")
})
public class FileAttachment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String originalName;

    @Column(nullable = false)
    private String savedName;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private long size;

    @Column(nullable = false)
    private String path; // 저장된 절대/상대 경로

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    protected FileAttachment() {}

    public FileAttachment(String originalName, String savedName, String contentType, long size, String path, Post post) {
        this.originalName = originalName;
        this.savedName = savedName;
        this.contentType = contentType;
        this.size = size;
        this.path = path;
        this.post = post;
    }

    public Long getId() { return id; }
    public String getOriginalName() { return originalName; }
    public String getSavedName() { return savedName; }
    public String getContentType() { return contentType; }
    public long getSize() { return size; }
    public String getPath() { return path; }
    public Post getPost() { return post; }
}
