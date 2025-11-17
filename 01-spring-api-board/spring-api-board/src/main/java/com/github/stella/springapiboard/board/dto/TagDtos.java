package com.github.stella.springapiboard.board.dto;

import com.github.stella.springapiboard.board.domain.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class TagDtos {

    public record TagDto(
            Long id,
            String name,
            String slug,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static TagDto from(Tag t) {
            return new TagDto(t.getId(), t.getName(), t.getSlug(), t.getCreatedAt(), t.getUpdatedAt());
        }
    }

    public record CreateTagRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Size(max = 120) String slug
    ) {}

    public record UpdateTagRequest(
            @Size(max = 100) String name,
            @Size(max = 120) String slug
    ) {}
}
