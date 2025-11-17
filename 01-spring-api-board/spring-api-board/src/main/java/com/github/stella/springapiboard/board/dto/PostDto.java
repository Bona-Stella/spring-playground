package com.github.stella.springapiboard.board.dto;

import com.github.stella.springapiboard.board.domain.Post;

import java.time.LocalDateTime;

public record PostDto(
        Long id,
        String title,
        String content,
        String author,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PostDto from(Post p) {
        return new PostDto(
                p.getId(),
                p.getTitle(),
                p.getContent(),
                p.getAuthor(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
