package com.github.stella.springsecurityjwt.auth.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken
) {}
