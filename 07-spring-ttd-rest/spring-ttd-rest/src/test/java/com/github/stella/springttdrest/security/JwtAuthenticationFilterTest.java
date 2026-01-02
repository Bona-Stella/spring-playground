package com.github.stella.springttdrest.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @AfterEach
    void tearDown() {
        // 테스트 후 컨텍스트 초기화 (다른 테스트 간섭 방지)
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("유효한 토큰이 헤더에 있으면 인증 객체가 SecurityContext에 저장된다")
    void doFilterInternal_valid_token() throws Exception {
        // given
        String token = "valid.jwt.token";
        String userId = "1";

        // 헤더에서 Bearer 토큰 추출 시뮬레이션
        given(request.getHeader("Authorization")).willReturn("Bearer " + token);

        // Provider 동작 모킹
        given(jwtTokenProvider.validateToken(token)).willReturn(true);
        given(jwtTokenProvider.getSubject(token)).willReturn(userId);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        // 1. 컨텍스트에 인증 정보가 있어야 함
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        // 2. 그 인증 정보의 이름(Principal)이 userId와 같아야 함
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo(userId);

        // 3. 다음 필터로 진행되어야 함
        verify(filterChain).doFilter(request, response);
    }
}