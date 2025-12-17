package com.github.stella.springmsamq.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                // 👇 1. CORS 설정 추가 (이게 없으면 프론트에서 403/Network Error 뜸)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                .authorizeExchange(exchange -> exchange
                        .pathMatchers("/health", "/actuator/health", "/api/auth/**", "/ws/**").permitAll()
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .build();
    }

    // 👇 2. 상세 CORS 정책 정의
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 허용할 출처 (프론트엔드 주소)
        // 개발 중엔 "*" (모두 허용) 하거나 구체적인 주소 적기
        configuration.setAllowedOrigins(List.of("http://localhost:3000", "http://127.0.0.1:3000"));

        // 허용할 HTTP 메서드
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // 허용할 헤더
        configuration.setAllowedHeaders(List.of("*"));

        // 인증 정보(쿠키, Authorization 헤더 등) 포함 허용 여부
        configuration.setAllowCredentials(true);

        // 모든 경로에 대해 위 설정 적용
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
