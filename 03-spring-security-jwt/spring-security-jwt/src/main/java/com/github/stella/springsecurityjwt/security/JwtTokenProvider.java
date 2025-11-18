package com.github.stella.springsecurityjwt.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenTtlMillis;
    private final long refreshTokenTtlMillis;

    public enum TokenType { ACCESS, REFRESH }

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-ttl-ms:1800000}") long accessTokenTtlMillis,
            @Value("${jwt.refresh-token-ttl-ms:1209600000}") long refreshTokenTtlMillis
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenTtlMillis = accessTokenTtlMillis;
        this.refreshTokenTtlMillis = refreshTokenTtlMillis;
    }

    public String createToken(String subject, List<String> roles, TokenType type) {
        long ttl = (type == TokenType.ACCESS) ? accessTokenTtlMillis : refreshTokenTtlMillis;
        Instant now = Instant.now();
        Instant exp = now.plusMillis(ttl);

        return Jwts.builder()
                .subject(subject)
                .claims(Map.of(
                        "roles", roles,
                        "type", type.name()
                ))
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(secretKey)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isAccessToken(String token) {
        return TokenType.ACCESS.name().equalsIgnoreCase(getType(token));
    }

    public boolean isRefreshToken(String token) {
        return TokenType.REFRESH.name().equalsIgnoreCase(getType(token));
    }

    public String getSubject(String token) {
        return parseClaims(token).getSubject();
    }

    public List<String> getRoles(String token) {
        Object roles = parseClaims(token).get("roles");
        if (roles instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    public String getType(String token) {
        Object type = parseClaims(token).get("type");
        return type == null ? null : type.toString();
    }

    public Instant getExpiration(String token) {
        Date exp = parseClaims(token).getExpiration();
        return exp == null ? null : exp.toInstant();
    }
}
