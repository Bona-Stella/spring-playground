package com.github.stella.springapiboard.common.api;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 페이징 응답을 위한 표준 DTO
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
