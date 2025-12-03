package com.github.stella.springmsamq.common;

/**
 * 공통 에러 응답 DTO (md/Error Response Specification.md 참조)
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
                java.time.ZonedDateTime.now().toString(),
                path
        );
    }
}
