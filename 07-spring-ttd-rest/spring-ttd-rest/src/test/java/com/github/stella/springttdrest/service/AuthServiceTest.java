package com.github.stella.springttdrest.service;

import com.github.stella.springttdrest.dto.LoginRequest;
import com.github.stella.springttdrest.dto.TokenResponse;
import com.github.stella.springttdrest.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("로그인 성공 - 유저 ID를 받으면 액세스 토큰을 반환한다")
    void login_success() {
        // given
        String userId = "1";
        String expectedToken = "access.token.string";
        LoginRequest request = new LoginRequest(userId, "password1234"); // 비밀번호는 일단 더미

        // 토큰 생성기가 "1"을 받으면 토큰을 리턴한다고 가정
        given(jwtTokenProvider.createToken(userId)).willReturn(expectedToken);

        // when
        TokenResponse response = authService.login(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo(expectedToken);
    }
}