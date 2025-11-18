package com.github.stella.springapiboard.board.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

public class PostSearchDtos {

    public record PostSearchCondition(
            String keyword,          // 제목/내용 검색어
            String author,           // 작성자 정확 일치
            Long categoryId,         // 카테고리 ID
            List<Long> tagIds,       // 포함할 태그 ID 목록(AND 조건)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {}

}
