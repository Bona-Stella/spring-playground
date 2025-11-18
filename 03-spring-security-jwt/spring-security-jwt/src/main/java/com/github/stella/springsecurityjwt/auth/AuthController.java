package com.github.stella.springsecurityjwt.auth;

import com.github.stella.springsecurityjwt.auth.dto.LoginRequest;
import com.github.stella.springsecurityjwt.auth.dto.RefreshRequest;
import com.github.stella.springsecurityjwt.auth.dto.SignupRequest;
import com.github.stella.springsecurityjwt.auth.dto.TokenResponse;
import com.github.stella.springsecurityjwt.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
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
                                                            HttpServletRequest request) {
        TokenResponse token = authService.login(requestDto);
        return ResponseEntity.ok(ApiResponse.success(token, request.getRequestURI()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(@Valid @RequestBody RefreshRequest requestDto,
                                                              HttpServletRequest request) {
        TokenResponse token = authService.refresh(requestDto);
        return ResponseEntity.ok(ApiResponse.success(token, request.getRequestURI()));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Map<String, Object>>> logout(Authentication authentication,
                                                                   HttpServletRequest request,
                                                                   @RequestHeader(name = org.springframework.http.HttpHeaders.AUTHORIZATION, required = false) String bearer) {
        String username = authentication.getName();
        String accessToken = null;
        if (bearer != null && bearer.startsWith("Bearer ")) {
            accessToken = bearer.substring(7);
        }
        authService.logout(username, accessToken);
        Map<String, Object> data = Map.of("logout", true);
        return ResponseEntity.ok(ApiResponse.success(data, request.getRequestURI()));
    }
}
