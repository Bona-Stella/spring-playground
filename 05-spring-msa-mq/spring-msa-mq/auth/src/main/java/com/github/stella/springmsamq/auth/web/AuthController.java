package com.github.stella.springmsamq.auth.web;

import com.github.stella.springmsamq.auth.domain.User;
import com.github.stella.springmsamq.auth.dto.LoginRequest;
import com.github.stella.springmsamq.auth.dto.SignupRequest;
import com.github.stella.springmsamq.auth.service.JwtService;
import com.github.stella.springmsamq.auth.service.JwtService.JwtPair;
import com.github.stella.springmsamq.auth.service.RedisTokenService;
import com.github.stella.springmsamq.auth.service.UserService;
import com.github.stella.springmsamq.common.ApiResponse;
import com.nimbusds.jwt.JWTClaimsSet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "REFRESH_TOKEN";

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RedisTokenService tokenService;
    private final com.github.stella.springmsamq.auth.service.RevokePublisher revokePublisher;

    public AuthController(UserService userService,
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService,
                          RedisTokenService tokenService,
                          com.github.stella.springmsamq.auth.service.RevokePublisher revokePublisher) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenService = tokenService;
        this.revokePublisher = revokePublisher;
    }

    @PostMapping(value = "/signup", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> signup(@RequestBody SignupRequest req, HttpServletRequest request) {
        User user = userService.signup(req.username(), req.password());
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "id", user.getId(),
                "username", user.getUsername()
        ), request.getRequestURI()));
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> login(@RequestBody LoginRequest req, HttpServletRequest request) {
        Optional<User> userOpt = userService.findByUsername(req.username());
        if (userOpt.isEmpty() || !passwordEncoder.matches(req.password(), userOpt.get().getPassword())) {
            return ResponseEntity.status(401).body(ApiResponse.of(401, "UNAUTHORIZED", "Invalid credentials", null, request.getRequestURI()));
        }
        User user = userOpt.get();
        JwtPair pair = jwtService.issue(user.getId(), user.getRoles());
        // Save refresh in Redis
        Duration refreshTtl = Duration.between(Instant.now(), pair.refreshExp());
        tokenService.saveRefreshToken(user.getId(), pair.refreshToken(), refreshTtl);

        // Build response with Access header and Refresh cookie
        ResponseCookie refreshCookie = ResponseCookie.from(REFRESH_COOKIE_NAME, pair.refreshToken())
                .httpOnly(true)
                .secure(false) // local dev; set true when using HTTPS
                .sameSite("None")
                .path("/")
                .maxAge(refreshTtl)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + pair.accessToken())
                .body(ApiResponse.success(Map.of(
                        "userId", user.getId(),
                        "roles", user.getRoles()
                ), request.getRequestURI()));
    }

    @PostMapping("/reissue")
    public ResponseEntity<?> reissue(@CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refresh,
                                     HttpServletRequest request) {
        if (refresh == null || !jwtService.verify(refresh)) {
            return ResponseEntity.status(401).body(ApiResponse.of(401, "UNAUTHORIZED", "Invalid refresh token", null, request.getRequestURI()));
        }
        JWTClaimsSet claims = jwtService.parseClaims(refresh);
        Long userId = Long.valueOf(claims.getSubject());
        String stored = tokenService.getRefreshToken(userId);
        if (stored == null || !stored.equals(refresh)) {
            return ResponseEntity.status(401).body(ApiResponse.of(401, "UNAUTHORIZED", "Refresh token not recognized", null, request.getRequestURI()));
        }

        // Rotation: issue new pair, overwrite Redis and cookie
        JwtPair pair = jwtService.issue(userId, String.valueOf(claims.getClaim("roles")));
        Duration refreshTtl = Duration.between(Instant.now(), pair.refreshExp());
        tokenService.saveRefreshToken(userId, pair.refreshToken(), refreshTtl);

        ResponseCookie refreshCookie = ResponseCookie.from(REFRESH_COOKIE_NAME, pair.refreshToken())
                .httpOnly(true)
                .secure(false)
                .sameSite("None")
                .path("/")
                .maxAge(refreshTtl)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + pair.accessToken())
                .body(ApiResponse.success(null, request.getRequestURI()));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                                    @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refresh,
                                    HttpServletRequest request) {
        // Remove refresh from Redis if present
        if (refresh != null) {
            if (jwtService.verify(refresh)) {
                JWTClaimsSet rc = jwtService.parseClaims(refresh);
                Long userId = Long.valueOf(rc.getSubject());
                tokenService.deleteRefreshToken(userId);
            }
        }
        // Blacklist access for remaining TTL
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String access = authorization.substring(7);
            if (jwtService.verify(access)) {
                JWTClaimsSet ac = jwtService.parseClaims(access);
                String jti = ac.getJWTID();
                Instant exp = ac.getExpirationTime().toInstant();
                Duration ttl = Duration.between(Instant.now(), exp);
                if (!ttl.isNegative()) {
                    tokenService.blacklistAccess(jti, ttl);
                    // 푸시형 블랙리스트: 게이트웨이에 즉시 통지
                    revokePublisher.publish(jti, exp.toEpochMilli());
                }
            }
        }

        // Expire cookie immediately
        ResponseCookie expired = ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true).secure(false).sameSite("None").path("/")
                .maxAge(Duration.ZERO).build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, expired.toString())
                .body(ApiResponse.success(null, request.getRequestURI()));
    }
}
