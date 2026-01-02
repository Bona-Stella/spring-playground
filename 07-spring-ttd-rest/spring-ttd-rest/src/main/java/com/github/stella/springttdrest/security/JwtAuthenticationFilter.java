package com.github.stella.springttdrest.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        // 1. Request Header에서 토큰 추출
        String token = resolveToken(request);

        // 2. 토큰 유효성 검사
        if (token != null && jwtTokenProvider.validateToken(token)) {
            // 3. 토큰에서 User ID 추출
            String userId = jwtTokenProvider.getSubject(token);

            // 4. 인증 객체 생성 (권한은 일단 비워둡니다)
            // Principal(userId), Credentials(null), Authorities(empty)
            Authentication authentication = new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());

            // 5. SecurityContext에 저장 (이제 Spring Security는 이 유저를 로그인된 상태로 인지함)
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // 6. 다음 필터로 이동
        filterChain.doFilter(request, response);
    }

    // "Bearer " 접두사를 제거하고 순수 토큰만 꺼내는 메서드
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}