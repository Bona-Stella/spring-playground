package com.github.stella.springmsamq.gateway.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 게이트웨이 프로세스 메모리에 유지되는 JTI Deny-Set.
 * 만료 시각(expEpochMillis)까지 차단한다.
 */
@Component
public class InMemoryDenyList {
    private final Map<String, Long> denied = new ConcurrentHashMap<>();

    public void put(String jti, long expEpochMillis) {
        denied.put(jti, expEpochMillis);
    }

    public boolean isDenied(String jti) {
        Long exp = denied.get(jti);
        if (exp == null) return false;
        if (exp <= Instant.now().toEpochMilli()) {
            denied.remove(jti);
            return false;
        }
        return true;
    }
}
