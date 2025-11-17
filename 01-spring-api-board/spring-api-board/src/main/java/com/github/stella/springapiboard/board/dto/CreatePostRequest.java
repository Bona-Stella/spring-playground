package com.github.stella.springapiboard.board.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreatePostRequest(
        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 200, message = "제목은 200자 이하이어야 합니다.")
        String title,

        @NotBlank(message = "내용은 필수입니다.")
        String content,

        @NotBlank(message = "작성자는 필수입니다.")
        @Size(max = 100, message = "작성자는 100자 이하이어야 합니다.")
        String author,

        // 선택: 카테고리/태그 지정
        Long categoryId,
        List<Long> tagIds
) {}
