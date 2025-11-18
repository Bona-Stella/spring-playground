package com.github.stella.springapiboard.board.dto;

import com.github.stella.springapiboard.board.domain.FileAttachment;

import java.time.LocalDateTime;

public class FileDtos {

    public record FileDto(
            Long id,
            String originalName,
            String savedName,
            String contentType,
            long size,
            String path,
            Long postId,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static FileDto from(FileAttachment fa) {
            return new FileDto(
                    fa.getId(),
                    fa.getOriginalName(),
                    fa.getSavedName(),
                    fa.getContentType(),
                    fa.getSize(),
                    fa.getPath(),
                    fa.getPost() == null ? null : fa.getPost().getId(),
                    fa.getCreatedAt(),
                    fa.getUpdatedAt()
            );
        }
    }
}
