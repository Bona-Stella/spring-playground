package com.github.stella.springaoplab.common.error;

import java.time.ZonedDateTime;

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
