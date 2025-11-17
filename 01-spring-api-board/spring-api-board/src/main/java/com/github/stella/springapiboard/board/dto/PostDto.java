package com.github.stella.springapiboard.board.dto;

import com.github.stella.springapiboard.board.domain.Post;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public record PostDto(
        Long id,
        String title,
        String content,
        String author,
        Long categoryId,
        List<Long> tagIds,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PostDto from(Post p) {
        return new PostDto(
                p.getId(),
                p.getTitle(),
                p.getContent(),
                p.getAuthor(),
                p.getCategory() == null ? null : p.getCategory().getId(),
                p.getTags() == null ? List.of() : p.getTags().stream().map(t -> t.getId()).collect(Collectors.toList()),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
