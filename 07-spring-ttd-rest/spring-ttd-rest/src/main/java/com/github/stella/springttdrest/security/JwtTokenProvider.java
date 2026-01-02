package com.github.stella.springttdrest.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final Key key;
    private final long validityInMilliseconds;

    public JwtTokenProvider(@Value("${jwt.secret}") String secretKey,
                            @Value("${jwt.expiration}") long validityInMilliseconds) {
        this.validityInMilliseconds = validityInMilliseconds;
        // 비밀키 문자열을 바이트 배열로 변환하여 Key 객체 생성 (HMAC-SHA 알고리즘용)
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    // 1. 토큰 생성
    public String createToken(String subject) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInMilliseconds);

        return Jwts.builder()
                .setSubject(subject) // 주로 userId 저장
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(key, SignatureAlgorithm.HS256) // 암호화 알고리즘 및 키 설정
                .compact();
    }

    // 2. 토큰에서 Subject(UserId) 추출
    public String getSubject(String token) {
        return parseClaims(token).getBody().getSubject();
    }

    // 3. 유효성 검증
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // 서명이 틀리거나, 만료되었거나, 형식이 잘못된 경우
            return false;
        }
    }

    // 공통 파싱 로직
    private Jws<Claims> parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
    }
}