package com.github.stella.springaoplab.common.error;

public enum ErrorCode {

    // 400 BAD REQUEST
    INVALID_INPUT(400, "잘못된 요청입니다."),
    VALIDATION_ERROR(400, "입력값이 올바르지 않습니다."),

    // 401 UNAUTHORIZED
    UNAUTHORIZED(401, "인증이 필요합니다."),
    INVALID_TOKEN(401, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(401, "만료된 토큰입니다."),

    // 403 FORBIDDEN
    ACCESS_DENIED(403, "접근 권한이 없습니다."),

    // 404 NOT FOUND
    NOT_FOUND(404, "요청한 리소스를 찾을 수 없습니다."),
    POST_NOT_FOUND(404, "해당 게시글을 찾을 수 없습니다."),

    // 409 CONFLICT
    DUPLICATE_RESOURCE(409, "이미 존재하는 리소스입니다."),

    // 500 INTERNAL SERVER ERROR
    INTERNAL_SERVER_ERROR(500, "서버 오류가 발생했습니다.");

    private final int status;
    private final String message;

    ErrorCode(int status, String message) {
        this.status = status;
        this.message = message;
    }

    public int status() { return status; }
    public String message() { return message; }
}
