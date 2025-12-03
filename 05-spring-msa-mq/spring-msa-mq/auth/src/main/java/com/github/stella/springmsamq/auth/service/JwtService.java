package com.github.stella.springmsamq.auth.service;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
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

    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;
    private final String issuer;
    private final long accessTtlSeconds;
    private final long refreshTtlSeconds;
    private final String privateKeyLocation;
    private final String publicKeyLocation;

    public JwtService(
            com.github.stella.springmsamq.auth.config.JwtProperties props,
            ResourceLoader resourceLoader
    ) {
        this.privateKeyLocation = props.getPrivateKeyLocation();
        this.publicKeyLocation = props.getPublicKeyLocation();
        this.issuer = props.getIssuer();
        this.accessTtlSeconds = props.getAccessTokenValiditySeconds();
        this.refreshTtlSeconds = props.getRefreshTokenValiditySeconds();
    }

    public JwtPair issue(Long userId, String roles) {
        Instant now = Instant.now();
        String jti = UUID.randomUUID().toString();
        Instant accessExp = now.plusSeconds(accessTtlSeconds);
        String access = sign(buildClaims(userId, roles, jti, now, accessExp));

        String refreshJti = UUID.randomUUID().toString();
        Instant refreshExp = now.plusSeconds(refreshTtlSeconds);
        String refresh = sign(buildClaims(userId, roles, refreshJti, now, refreshExp));

        return new JwtPair(access, refresh, jti, accessExp, refreshExp);
    }

    public boolean verify(String token) {
        try {
            ensureKeysLoaded();
            SignedJWT jwt = SignedJWT.parse(token);
            JWSVerifier verifier = new RSASSAVerifier(publicKey);
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
            ensureKeysLoaded();
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).build();
            SignedJWT signedJWT = new SignedJWT(header, claims);
            JWSSigner signer = new RSASSASigner(privateKey);
            signedJWT.sign(signer);
            return signedJWT.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign JWT", e);
        }
    }

    private void ensureKeysLoaded() throws Exception {
        if (privateKey == null || publicKey == null) {
            ResourceLoader loader = new org.springframework.core.io.DefaultResourceLoader();
            this.privateKey = (RSAPrivateKey) loadPrivateKey(loader.getResource(privateKeyLocation));
            this.publicKey = (RSAPublicKey) loadPublicKey(loader.getResource(publicKeyLocation));
        }
    }

    private static PrivateKey loadPrivateKey(Resource resource) throws Exception {
        String pem = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8)
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\r?\n", "");
        byte[] keyBytes = Base64.getDecoder().decode(pem);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        return KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    private static PublicKey loadPublicKey(Resource resource) throws Exception {
        String pem = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8)
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\r?\n", "");
        byte[] keyBytes = Base64.getDecoder().decode(pem);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        return KeyFactory.getInstance("RSA").generatePublic(spec);
    }
}
