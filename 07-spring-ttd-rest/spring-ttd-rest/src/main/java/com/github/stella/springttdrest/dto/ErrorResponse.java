package com.github.stella.springttdrest.dto;

public record ErrorResponse(
        String code,
        String message
) {
}