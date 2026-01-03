package com.github.stella.springttdrest.config;

import com.github.stella.springttdrest.security.JwtAuthenticationFilter;
import com.github.stella.springttdrest.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. CSRF 비활성화 (JWT 사용 시 필수)
                .csrf(AbstractHttpConfigurer::disable)

                // 2. 세션 사용 안 함 (Stateless)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 3. URL 관리
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**",
                                "/swagger-ui/**", // Swagger UI 페이지
                                "/v3/api-docs/**" // Swagger API JSON 데이터
                        ).permitAll() // 로그인, 회원가입은 누구나 접근 가능
                        .anyRequest().authenticated()            // 나머지는 인증 필요
                )

                // 4. JWT 필터 등록 (UsernamePasswordAuthenticationFilter 앞에 추가)
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}