package com.github.stella.springaoplab.post.dto;

import com.github.stella.springaoplab.post.domain.Post;

import java.time.LocalDateTime;

public record PostDto(Long id, String title, String content, LocalDateTime createdAt) {
    public static PostDto from(Post p) {
        return new PostDto(p.getId(), p.getTitle(), p.getContent(), p.getCreatedAt());
    }
}
