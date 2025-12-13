package com.github.stella.springmsamq.auth.service;

import com.github.stella.springmsamq.auth.config.JwtProperties;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    public record JwtPair(String accessToken, String refreshToken, String accessJti, Instant accessExp, Instant refreshExp) {}

    // 불변성 유지를 위해 final 권장하지만, PostConstruct 초기화를 위해 일반 필드로 둡니다.
    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;

    private final String issuer;
    private final long accessTtlSeconds;
    private final long refreshTtlSeconds;
    private final String privateKeyLocation;
    private final String publicKeyLocation;

    // 리소스 로더를 필드로 저장하여 재사용
    private final ResourceLoader resourceLoader;

    public JwtService(JwtProperties props, ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
        this.privateKeyLocation = props.getPrivateKeyLocation();
        this.publicKeyLocation = props.getPublicKeyLocation();
        this.issuer = props.getIssuer();
        this.accessTtlSeconds = props.getAccessTokenValiditySeconds();
        this.refreshTtlSeconds = props.getRefreshTokenValiditySeconds();
    }

    /**
     * 서버 시작 시점에 키 파일을 로드합니다.
     * 파일이 없거나 잘못되었으면 애플리케이션 시작 자체가 실패합니다. (Fail-Fast)
     */
    @PostConstruct
    public void init() throws Exception {
        this.privateKey = (RSAPrivateKey) loadKey(privateKeyLocation, true);
        this.publicKey = (RSAPublicKey) loadKey(publicKeyLocation, false);
    }

    public JwtPair issue(Long userId, String roles) {
        Instant now = Instant.now();

        // Access Token
        String jti = UUID.randomUUID().toString();
        Instant accessExp = now.plusSeconds(accessTtlSeconds);
        String access = sign(buildClaims(userId, roles, jti, now, accessExp));

        // Refresh Token
        String refreshJti = UUID.randomUUID().toString();
        Instant refreshExp = now.plusSeconds(refreshTtlSeconds);
        String refresh = sign(buildClaims(userId, roles, refreshJti, now, refreshExp));

        return new JwtPair(access, refresh, jti, accessExp, refreshExp);
    }

    public boolean verify(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            JWSVerifier verifier = new RSASSAVerifier(publicKey);
            // 서명 검증 및 만료 시간 확인
            return jwt.verify(verifier) && jwt.getJWTClaimsSet().getExpirationTime().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    public JWTClaimsSet parseClaims(String token) {
        try {
            return SignedJWT.parse(token).getJWTClaimsSet();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid token", e);
        }
    }

    private JWTClaimsSet buildClaims(Long userId, String roles, String jti, Instant iat, Instant exp) {
        return new JWTClaimsSet.Builder()
                .subject(String.valueOf(userId))
                .issuer(issuer)
                .claim("roles", roles)
                .jwtID(jti)
                .issueTime(Date.from(iat))
                .expirationTime(Date.from(exp))
                .build();
    }

    private String sign(JWTClaimsSet claims) {
        try {
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .type(JOSEObjectType.JWT)
                    .build();
            SignedJWT signedJWT = new SignedJWT(header, claims);
            JWSSigner signer = new RSASSASigner(privateKey);
            signedJWT.sign(signer);
            return signedJWT.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign JWT", e);
        }
    }

    /**
     * 통합 키 로딩 메서드 (Private/Public 공용)
     */
    private Key loadKey(String path, boolean isPrivate) throws Exception {
        Resource resource = resourceLoader.getResource(path);

        String pem;
        try (var is = resource.getInputStream()) {
            pem = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }

        // 정규식을 사용하여 헤더, 푸터, 공백, 개행문자를 한 번에 제거
        // "-----"로 시작하고 끝나는 모든 라인 제거 & 모든 공백(\s) 제거
        String keyContent = pem.replaceAll("-----BEGIN [A-Z ]+-----", "")
                .replaceAll("-----END [A-Z ]+-----", "")
                .replaceAll("\\s+", "");

        byte[] keyBytes = Base64.getDecoder().decode(keyContent);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        if (isPrivate) {
            return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
        } else {
            return keyFactory.generatePublic(new X509EncodedKeySpec(keyBytes));
        }
    }
}