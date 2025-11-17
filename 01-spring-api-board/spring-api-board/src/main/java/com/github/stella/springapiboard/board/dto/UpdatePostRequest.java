package com.github.stella.springapiboard.board.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdatePostRequest(
        @Size(max = 200, message = "제목은 200자 이하이어야 합니다.")
        String title,

        String content,

        // 선택 변경: 카테고리/태그
        Long categoryId,
        List<Long> tagIds
) {}
