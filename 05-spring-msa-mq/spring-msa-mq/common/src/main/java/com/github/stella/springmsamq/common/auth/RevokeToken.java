package com.github.stella.springmsamq.common.auth;

/**
 * 게이트웨이 인메모리 블랙리스트를 위한 토큰 폐기 이벤트 페이로드
 */
public record RevokeToken(
        String jti,
        long expEpochMillis
) {}
