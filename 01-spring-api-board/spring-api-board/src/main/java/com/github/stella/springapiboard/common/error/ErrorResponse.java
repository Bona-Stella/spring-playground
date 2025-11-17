package com.github.stella.springapiboard.common.error;

import java.time.ZonedDateTime;

/**
 * 전역 에러 응답 DTO
 */
public record ErrorResponse(
        boolean success,
        int status,
        String code,
        String message,
        String timestamp,
        String path
) {
    public static ErrorResponse of(ErrorCode code, String path) {
        return new ErrorResponse(
                false,
                code.status(),
                code.name(),
                code.message(),
                ZonedDateTime.now().toString(),
                path
        );
    }
}
