package com.github.stella.springsecurityjwt.security.filter;

import com.github.stella.springsecurityjwt.common.error.CustomException;
import com.github.stella.springsecurityjwt.common.error.ErrorCode;
import com.github.stella.springsecurityjwt.security.RedisTokenService;
import com.github.stella.springsecurityjwt.security.jwt.JwtProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final RedisTokenService redisTokenService;

    public JwtAuthenticationFilter(JwtProvider jwtProvider, RedisTokenService redisTokenService) {
        this.jwtProvider = jwtProvider;
        this.redisTokenService = redisTokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = resolveToken(request);

        // 1. 토큰이 존재할 때만 검증 로직 수행
        if (token != null) {

            // 2. 블랙리스트 확인 (Redis 조회)
            if (redisTokenService.isBlacklisted(token)) {
                throw new CustomException(ErrorCode.INVALID_TOKEN);
            }

            // 3. 토큰 파싱 (여기서 위변조, 만료 체크 수행됨 -> 예외 발생 시 상위 필터로 던짐)
            // 성능 최적화: 여기서 얻은 claims 객체를 계속 재사용함
            Claims claims = jwtProvider.parseClaims(token).getBody();

            // 4. Access Token일 경우에만 인증 처리 (Refresh Token은 무시)
            if (jwtProvider.isAccessToken(claims)) {
                Authentication authentication = jwtProvider.getAuthentication(claims);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        // 5. 다음 필터로 진행 (인증이 안 된 경우에도 Anonymous 상태로 진행되어야 함)
        filterChain.doFilter(request, response);
    }

    // 헤더 추출 로직 분리
    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
