package com.github.stella.springttdrest.service;

import com.github.stella.springttdrest.dto.LoginRequest;
import com.github.stella.springttdrest.dto.TokenResponse;
import com.github.stella.springttdrest.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtTokenProvider jwtTokenProvider;

    public TokenResponse login(LoginRequest request) {
        // 실무에서는 여기서 DB를 조회하여 비밀번호 검증을 해야 합니다.
        // 현재는 학습 목적상 ID가 곧 유효한 유저라고 가정하고 바로 토큰을 발급합니다.

        String token = jwtTokenProvider.createToken(request.username());

        return new TokenResponse(token);
    }
}