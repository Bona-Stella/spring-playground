package com.github.stella.springsecurityjwt.security.jwt;

import com.github.stella.springsecurityjwt.common.error.CustomException;
import com.github.stella.springsecurityjwt.common.error.ErrorCode;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class JwtProvider {
    private final JwtProperties props;
    private final Key key;

    private static final String KEY_ROLES = "roles";
    private static final String KEY_TYPE = "type";

    public JwtProvider(JwtProperties props) {
        this.props = props;
        // 보안: OS 기본 인코딩에 의존하지 않고 UTF-8 명시
        this.key = Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    // 1. 토큰 생성
    public String generateToken(String subject, List<String> roles, TokenType type) {
        long validity = type == TokenType.ACCESS ? props.getAccessTokenValiditySeconds() : props.getRefreshTokenValiditySeconds();
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(validity);

        return Jwts.builder()
                .setSubject(subject)
                .claim(KEY_ROLES, roles)
                .claim(KEY_TYPE, type.name())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // 2. 토큰 파싱 (예외 처리 포함)
    // 검증과 동시에 Claims를 반환하므로, 외부에서 여러 번 호출하지 않도록 주의
    public Jws<Claims> parseClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
        } catch (ExpiredJwtException e) {
            throw new CustomException(ErrorCode.EXPIRED_TOKEN);
        } catch (JwtException | IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
    }

    // 3. Authentication 객체 생성 (필터의 부담을 줄임)
    // 이미 파싱된 Claims를 받아서 처리 (중복 파싱 방지)
    public Authentication getAuthentication(Claims claims) {
        String subject = claims.getSubject();
        List<String> roles = getRoles(claims);

        List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role) // 접두사 처리 캡슐화
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        // 비밀번호는 불필요하므로 null 또는 빈 문자열
        return new UsernamePasswordAuthenticationToken(subject, null, authorities);
    }

    // 4. Access Token 여부 확인 (Claims 기반)
    public boolean isAccessToken(Claims claims) {
        return TokenType.ACCESS.name().equals(claims.get("type"));
    }

    // 5. 역할 정보 추출 (Helper)
    @SuppressWarnings("unchecked")
    public List<String> getRoles(Claims claims) {
        List<?> rawRoles = claims.get(KEY_ROLES, List.class);
        if (rawRoles == null) {
            return List.of();
        }
        return rawRoles.stream()
                .map(Object::toString)
                .collect(Collectors.toList());
    }

    public boolean isRefreshToken(Claims claims) {
        return TokenType.REFRESH.name().equals(claims.get("type"));
    }

    public Instant getExpiration(String token) {
        return parseClaims(token).getBody().getExpiration().toInstant();
    }

    public long getRemainingSeconds(String token) {
        Instant now = Instant.now();
        Instant exp = getExpiration(token);
        long diff = exp.getEpochSecond() - now.getEpochSecond();
        return Math.max(diff, 0);
    }

    public String getSubject(String token) {
        return parseClaims(token).getBody().getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<String> getRoles(String token) {
        return (List<String>) parseClaims(token).getBody().get("roles", List.class);
    }
}
