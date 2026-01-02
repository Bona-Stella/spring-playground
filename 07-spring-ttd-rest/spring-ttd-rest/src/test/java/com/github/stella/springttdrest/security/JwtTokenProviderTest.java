package com.github.stella.springttdrest.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private final String secretKey = "v3ryS3cr3tK3yF0rSpriNgB00tTddPr0j3ct!!!"; // 32자 이상
    private final long validityInMilliseconds = 3600000; // 1h

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(secretKey, validityInMilliseconds);
    }

    @Test
    @DisplayName("토큰 생성 및 검증 - 유효한 토큰을 만들고 검증하면 true를 반환한다")
    void createToken_and_validate() {
        // given
        String userId = "1"; // 유저 ID (String)

        // when
        String token = jwtTokenProvider.createToken(userId);

        // then
        assertThat(token).isNotNull();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("토큰 파싱 - 토큰에서 유저 ID(Subject)를 추출할 수 있다")
    void getPayload_from_token() {
        // given
        String userId = "1";
        String token = jwtTokenProvider.createToken(userId);

        // when
        String extractedSubject = jwtTokenProvider.getSubject(token);

        // then
        assertThat(extractedSubject).isEqualTo(userId);
    }

    @Test
    @DisplayName("유효하지 않은 토큰 검증 - 위조된 토큰은 false를 반환한다")
    void validate_invalid_token() {
        // given
        // 다른 키로 서명된 토큰 생성
        Key otherKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        String invalidToken = Jwts.builder()
                .setSubject("1")
                .signWith(otherKey, SignatureAlgorithm.HS256)
                .compact();

        // when
        boolean isValid = jwtTokenProvider.validateToken(invalidToken);

        // then
        assertThat(isValid).isFalse();
    }
}