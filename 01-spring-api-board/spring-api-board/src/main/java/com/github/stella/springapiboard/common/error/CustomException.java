package com.github.stella.springapiboard.common.error;

/**
 * 도메인 전반에서 사용하는 런타임 예외
 */
public class CustomException extends RuntimeException {
    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.message());
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
