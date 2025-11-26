package com.github.stella.springsecurityjwt.auth;

import com.github.stella.springsecurityjwt.auth.dto.AuthDtos.LoginRequest;
import com.github.stella.springsecurityjwt.auth.dto.AuthDtos.RegisterRequest;
import com.github.stella.springsecurityjwt.auth.dto.AuthDtos.TokenResponse;
import com.github.stella.springsecurityjwt.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<TokenResponse>> register(@Valid @RequestBody RegisterRequest requestDto,
                                                               HttpServletRequest httpRequest) {
        TokenResponse tokens = authService.register(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(tokens, httpRequest.getRequestURI()));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest requestDto,
                                                            HttpServletRequest httpRequest) {
        TokenResponse tokens = authService.login(requestDto);
        return ResponseEntity.ok(ApiResponse.success(tokens, httpRequest.getRequestURI()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(@RequestHeader(name = "Authorization") String authorization,
                                                              HttpServletRequest httpRequest) {
        String token = extractBearer(authorization);
        TokenResponse tokens = authService.refresh(token);
        return ResponseEntity.ok(ApiResponse.success(tokens, httpRequest.getRequestURI()));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader(name = "Authorization") String authorization,
                                                    HttpServletRequest httpRequest) {
        String accessToken = extractBearer(authorization);
        authService.logout(accessToken);
        return ResponseEntity.ok(ApiResponse.success(null, httpRequest.getRequestURI()));
    }

    private String extractBearer(String header) {
        if (header == null || !header.startsWith("Bearer ")) {
            return header; // let service validate
        }
        return header.substring(7);
    }
}
