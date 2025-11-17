package com.github.stella.springapiboard.board.dto;

import com.github.stella.springapiboard.board.domain.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class CategoryDtos {

    public record CategoryDto(
            Long id,
            String name,
            String slug,
            String description,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static CategoryDto from(Category c) {
            return new CategoryDto(
                    c.getId(), c.getName(), c.getSlug(), c.getDescription(), c.getCreatedAt(), c.getUpdatedAt()
            );
        }
    }

    public record CreateCategoryRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Size(max = 120) String slug,
            @Size(max = 255) String description
    ) {}

    public record UpdateCategoryRequest(
            @Size(max = 100) String name,
            @Size(max = 120) String slug,
            @Size(max = 255) String description
    ) {}
}
