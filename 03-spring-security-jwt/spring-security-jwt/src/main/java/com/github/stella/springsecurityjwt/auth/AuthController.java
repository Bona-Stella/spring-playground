package com.github.stella.springsecurityjwt.auth;

import com.github.stella.springsecurityjwt.auth.dto.LoginRequest;
import com.github.stella.springsecurityjwt.auth.dto.RefreshRequest;
import com.github.stella.springsecurityjwt.auth.dto.SignupRequest;
import com.github.stella.springsecurityjwt.auth.dto.TokenResponse;
import com.github.stella.springsecurityjwt.common.api.ApiResponse;
import com.github.stella.springsecurityjwt.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider tokenProvider;

    public AuthController(AuthService authService, JwtTokenProvider tokenProvider) {
        this.authService = authService;
        this.tokenProvider = tokenProvider;
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Map<String, Object>>> signup(@Valid @RequestBody SignupRequest requestDto,
                                                                   HttpServletRequest request) {
        authService.signup(requestDto);
        Map<String, Object> data = Map.of("username", requestDto.username());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(data, request.getRequestURI()));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest requestDto,
                                                            HttpServletRequest request,
                                                            HttpServletResponse response) {
        TokenResponse token = authService.login(requestDto);
        // Set HttpOnly cookies for access & refresh
        setTokenCookies(response, token);
        return ResponseEntity.ok(ApiResponse.success(token, request.getRequestURI()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(@RequestBody(required = false) RefreshRequest requestDto,
                                                              HttpServletRequest request,
                                                              HttpServletResponse response) {
        // Allow refresh token from body or HttpOnly cookie
        if (requestDto == null || requestDto.refreshToken() == null || requestDto.refreshToken().isBlank()) {
            String fromCookie = resolveCookie(request, "REFRESH_TOKEN");
            if (fromCookie != null) {
                requestDto = new RefreshRequest(fromCookie);
            }
        }
        TokenResponse token = authService.refresh(requestDto);
        // rotate cookies
        setTokenCookies(response, token);
        return ResponseEntity.ok(ApiResponse.success(token, request.getRequestURI()));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Map<String, Object>>> logout(Authentication authentication,
                                                                   HttpServletRequest request,
                                                                   HttpServletResponse response,
                                                                   @RequestHeader(name = org.springframework.http.HttpHeaders.AUTHORIZATION, required = false) String bearer) {
        String username = authentication.getName();
        String accessToken = null;
        if (bearer != null && bearer.startsWith("Bearer ")) {
            accessToken = bearer.substring(7);
        }
        authService.logout(username, accessToken);
        // clear cookies
        clearTokenCookies(response);
        Map<String, Object> data = Map.of("logout", true);
        return ResponseEntity.ok(ApiResponse.success(data, request.getRequestURI()));
    }

    private void setTokenCookies(HttpServletResponse response, TokenResponse token) {
        // In dev, Secure=false. For production behind HTTPS, set secure=true and SameSite=None as needed.
        boolean secure = false;
        // ACCESS cookie: short-lived, HttpOnly, Path=/, SameSite=Lax
        long accessMaxAge = maxAgeSeconds(token.accessToken());
        ResponseCookie accessCookie = ResponseCookie.from("ACCESS_TOKEN", token.accessToken())
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .sameSite("Lax")
                .maxAge(accessMaxAge > 0 ? Duration.ofSeconds(accessMaxAge) : null)
                .build();
        // REFRESH cookie: longer-lived
        long refreshMaxAge = maxAgeSeconds(token.refreshToken());
        ResponseCookie refreshCookie = ResponseCookie.from("REFRESH_TOKEN", token.refreshToken())
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .sameSite("Lax")
                .maxAge(refreshMaxAge > 0 ? Duration.ofSeconds(refreshMaxAge) : null)
                .build();
        response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }

    private void clearTokenCookies(HttpServletResponse response) {
        boolean secure = false;
        ResponseCookie clearAccess = ResponseCookie.from("ACCESS_TOKEN", "")
                .httpOnly(true).secure(secure).path("/").sameSite("Lax").maxAge(Duration.ZERO).build();
        ResponseCookie clearRefresh = ResponseCookie.from("REFRESH_TOKEN", "")
                .httpOnly(true).secure(secure).path("/").sameSite("Lax").maxAge(Duration.ZERO).build();
        response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, clearAccess.toString());
        response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, clearRefresh.toString());
    }

    private long maxAgeSeconds(String jwt) {
        try {
            java.time.Instant exp = tokenProvider.getExpiration(jwt);
            if (exp == null) return -1;
            long seconds = java.time.Duration.between(java.time.Instant.now(), exp).getSeconds();
            return Math.max(0, seconds);
        } catch (Exception e) {
            return -1;
        }
    }

    private String resolveCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        for (jakarta.servlet.http.Cookie c : request.getCookies()) {
            if (name.equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }
}
