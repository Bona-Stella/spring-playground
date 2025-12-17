package com.github.stella.springmsamq.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component // 👈 이렇게 하면 스프링이 자동으로 필터로 인식합니다.
public class UserHeaderFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1. 보안 컨텍스트에서 인증 정보(Authentication)를 비동기로 꺼냅니다.
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication())
                .filter(auth -> auth.getPrincipal() instanceof Jwt) // JWT 토큰 방식인지 확인
                .map(auth -> (Jwt) auth.getPrincipal()) // JWT 객체로 형변환
                .flatMap(jwt -> {
                    // 2. 토큰에서 'sub' (Subject = 유저 ID) 값을 꺼냅니다.
                    // (만약 토큰 만들 때 'userId'라는 이름으로 넣었다면 jwt.getClaimAsString("userId")로 고치세요)
                    String userId = jwt.getSubject();

                    // 3. 기존 요청(Request)을 조작(Mutation)해서 헤더를 추가합니다.
                    ServerWebExchange mutatedExchange = exchange.mutate()
                            .request(builder -> builder.header("X-User-Id", userId))
                            .build();

                    log.debug("✅ 헤더 주입 완료: X-User-Id = {}", userId);

                    // 4. 헤더가 추가된 요청으로 다음 단계(Order 서비스)로 넘어갑니다.
                    return chain.filter(mutatedExchange);
                })
                // 인증 정보가 없거나 JWT가 아니면(로그인 안 한 요청 등) 그냥 원본 그대로 통과시킵니다.
                // (어차피 SecurityConfig에서 인증 안 된 요청은 막히거나, Order 서비스에서 401이 뜰 겁니다)
                .switchIfEmpty(chain.filter(exchange));
    }

    @Override
    public int getOrder() {
        // 필터 실행 순서: 인증 처리 직후에 실행되도록 우선순위를 잡습니다.
        return -1;
    }
}
