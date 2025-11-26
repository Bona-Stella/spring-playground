package com.github.stella.springsecurityjwt.auth.dto;

import jakarta.validation.constraints.NotBlank;

public class AuthDtos {
    public record RegisterRequest(
            @NotBlank String username,
            @NotBlank String password
    ) {}

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password
    ) {}

    public record TokenResponse(
            String accessToken,
            String refreshToken
    ) {}
}
