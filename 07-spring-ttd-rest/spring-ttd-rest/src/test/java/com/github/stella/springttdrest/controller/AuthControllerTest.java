package com.github.stella.springttdrest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.stella.springttdrest.config.SecurityConfig; // ★ Config import 필요
import com.github.stella.springttdrest.dto.LoginRequest;
import com.github.stella.springttdrest.dto.TokenResponse;
import com.github.stella.springttdrest.security.JwtTokenProvider;
import com.github.stella.springttdrest.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import; // ★ Import 어노테이션
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class) // ★ 핵심: 우리가 만든 보안 설정을 가져와야 permitAll이 적용됩니다.
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    // SecurityConfig를 가져왔으니, 거기서 주입받는 JwtTokenProvider도 빈으로 있어야 합니다.
    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("로그인 API - 성공 시 토큰을 반환한다")
    void login_success() throws Exception {
        // given
        LoginRequest request = new LoginRequest("1", "password");
        TokenResponse response = new TokenResponse("access.token.string");

        given(authService.login(request)).willReturn(response);

        // when & then
        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk()) // 이제 200 OK가 뜰 것입니다.
                .andExpect(jsonPath("$.accessToken").value("access.token.string"));
    }
}