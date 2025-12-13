package com.github.stella.springmsamq.auth.web;

import com.github.stella.springmsamq.auth.domain.User;
import com.github.stella.springmsamq.auth.dto.LoginRequest;
import com.github.stella.springmsamq.auth.dto.SignupRequest;
import com.github.stella.springmsamq.auth.service.JwtService;
import com.github.stella.springmsamq.auth.service.RedisTokenService;
import com.github.stella.springmsamq.auth.service.UserService;
import com.github.stella.springmsamq.auth.service.RevokePublisher;
import com.github.stella.springmsamq.common.ApiResponse;
import com.nimbusds.jwt.JWTClaimsSet;
import jakarta.servlet.http.HttpServletRequest;
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
    private final RevokePublisher revokePublisher;

    public AuthController(UserService userService,
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService,
                          RedisTokenService tokenService,
                          RevokePublisher revokePublisher) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenService = tokenService;
        this.revokePublisher = revokePublisher;
    }

    /**
     * 회원가입 엔드포인트
     */
    @PostMapping(value = "/signup", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> signup(@RequestBody SignupRequest req, HttpServletRequest request) {
        User user = userService.signup(req.username(), req.password());
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "id", user.getId(),
                "username", user.getUsername()
        ), request.getRequestURI()));
    }

    /**
     * 로그인 엔드포인트
     * 1. ID/PW 검증
     * 2. Access Token(Body), Refresh Token(Cookie) 발급
     * 3. Refresh Token Redis 저장
     */
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> login(@RequestBody LoginRequest req, HttpServletRequest request) {
        // 사용자 조회 및 비밀번호 검증
        Optional<User> userOpt = userService.findByUsername(req.username());
        if (userOpt.isEmpty() || !passwordEncoder.matches(req.password(), userOpt.get().getPassword())) {
            // 보안상 "아이디가 없다"와 "비번이 틀렸다"를 구분하지 않는 것이 좋습니다.
            return ResponseEntity.status(401).body(ApiResponse.of(401, "UNAUTHORIZED", "Invalid credentials", null, request.getRequestURI()));
        }

        User user = userOpt.get();

        // 토큰 발급 (리팩토링된 JwtService 사용)
        JwtService.JwtPair pair = jwtService.issue(user.getId(), user.getRoles());

        // Refresh Token Redis에 저장 (만료 시간 설정)
        Duration refreshTtl = Duration.between(Instant.now(), pair.refreshExp());
        tokenService.saveRefreshToken(user.getId(), pair.refreshToken(), refreshTtl);

        // 쿠키 생성 (리팩토링: 유틸 메서드 사용)
        ResponseCookie refreshCookie = createCookie(pair.refreshToken(), refreshTtl);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + pair.accessToken())
                .body(ApiResponse.success(Map.of(
                        "userId", user.getId(),
                        "roles", user.getRoles()
                ), request.getRequestURI()));
    }

    /**
     * 토큰 재발급(Rotation) 엔드포인트
     * Access Token 만료 시, 쿠키에 있는 Refresh Token을 이용해 새로 발급받음
     */
    @PostMapping("/reissue")
    public ResponseEntity<?> reissue(@CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refresh,
                                     HttpServletRequest request) {
        // 1. 쿠키 존재 여부 및 JWT 서명 검증
        if (refresh == null || !jwtService.verify(refresh)) {
            return ResponseEntity.status(401).body(ApiResponse.of(401, "UNAUTHORIZED", "Invalid or missing refresh token", null, request.getRequestURI()));
        }

        JWTClaimsSet claims = jwtService.parseClaims(refresh);
        Long userId = Long.valueOf(claims.getSubject());

        // 2. Redis에 저장된 토큰과 비교 (RTR: Refresh Token Rotation 정책)
        // 만약 DB에 없거나 값이 다르면, 탈취된 토큰일 수 있으므로 거부
        String stored = tokenService.getRefreshToken(userId);
        if (stored == null || !stored.equals(refresh)) {
            return ResponseEntity.status(401).body(ApiResponse.of(401, "UNAUTHORIZED", "Refresh token reused or revoked", null, request.getRequestURI()));
        }

        // 3. 새로운 토큰 쌍(Pair) 발급 (기존 토큰 폐기 효과)
        JwtService.JwtPair pair = jwtService.issue(userId, String.valueOf(claims.getClaim("roles")));
        Duration refreshTtl = Duration.between(Instant.now(), pair.refreshExp());

        // 4. Redis 업데이트 (덮어쓰기)
        tokenService.saveRefreshToken(userId, pair.refreshToken(), refreshTtl);

        ResponseCookie refreshCookie = createCookie(pair.refreshToken(), refreshTtl);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + pair.accessToken())
                .body(ApiResponse.success(null, request.getRequestURI()));
    }

    /**
     * 로그아웃 엔드포인트
     * 1. Refresh Token 삭제 (재발급 불가)
     * 2. Access Token 블랙리스트 등록 (현재 유효한 토큰 무력화)
     * 3. Gateway에 전파 (다른 서비스 접근 차단)
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                                    @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refresh,
                                    HttpServletRequest request) {
        // 1. Refresh Token 처리: 검증 후 Redis에서 삭제
        if (refresh != null && jwtService.verify(refresh)) {
            JWTClaimsSet rc = jwtService.parseClaims(refresh);
            Long userId = Long.valueOf(rc.getSubject());
            tokenService.deleteRefreshToken(userId);
        }

        // 2. Access Token 처리: 블랙리스트 등록
        String accessToken = resolveToken(authorization); // 리팩토링: 안전한 토큰 추출
        if (accessToken != null && jwtService.verify(accessToken)) {
            JWTClaimsSet ac = jwtService.parseClaims(accessToken);
            String jti = ac.getJWTID();
            Instant exp = ac.getExpirationTime().toInstant();
            Duration ttl = Duration.between(Instant.now(), exp);

            // 만료되지 않은 토큰만 블랙리스트에 등록
            if (!ttl.isNegative()) {
                // Auth 서비스 내 Redis 차단
                tokenService.blacklistAccess(jti, ttl);
                // 중요: Gateway 등 다른 서비스들에게 "이 토큰 막아라" 전파 (Pub/Sub)
                revokePublisher.publish(jti, exp.toEpochMilli());
            }
        }

        // 3. 클라이언트 쿠키 삭제 (만료시간 0으로 덮어쓰기)
        ResponseCookie expiredCookie = createCookie("", Duration.ZERO);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, expiredCookie.toString())
                .body(ApiResponse.success(null, request.getRequestURI()));
    }

    /**
     * [리팩토링] Authorization 헤더에서 'Bearer ' 접두어를 안전하게 제거하고 토큰만 추출
     */
    private String resolveToken(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return null;
    }

    /**
     * [리팩토링] 쿠키 생성 로직 통합
     * 주의: 로컬 개발 환경(HTTP)과 배포 환경(HTTPS)에 따라 Secure 설정이 달라져야 함.
     */
    private ResponseCookie createCookie(String value, Duration maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, value)
                .httpOnly(true)       // JavaScript에서 접근 불가 (XSS 방어)
                .secure(false)        // TODO: 운영 환경(HTTPS)에서는 true로 변경 필수
                .sameSite("Lax")      // None 사용 시 Secure=true 필수. 로컬 개발엔 Lax가 적합.
                .path("/")
                .maxAge(maxAge)
                .build();
    }
}
