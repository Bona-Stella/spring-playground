package com.github.stella.springredisjob.common.error;

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
