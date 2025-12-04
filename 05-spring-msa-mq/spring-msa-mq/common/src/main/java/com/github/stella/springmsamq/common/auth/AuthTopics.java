package com.github.stella.springmsamq.common.auth;

public final class AuthTopics {
    private AuthTopics() {}
    public static final String REDIS_CHANNEL_REVOKE = "auth:revoke"; // 액세스 토큰 JTI 폐기 이벤트
}
