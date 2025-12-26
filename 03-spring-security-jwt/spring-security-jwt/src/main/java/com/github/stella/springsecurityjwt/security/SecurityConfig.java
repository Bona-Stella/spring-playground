package com.github.stella.springsecurityjwt.security;

import com.github.stella.springsecurityjwt.security.filter.ExceptionHandlingFilter;
import com.github.stella.springsecurityjwt.security.filter.JwtAuthenticationFilter;
import com.github.stella.springsecurityjwt.security.jwt.JwtProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.core.annotation.Order;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder(PasswordProperties passwordProperties) {
        return new BCryptPasswordEncoder(passwordProperties.getBcryptStrength());
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new DelegatingSecurityContextRepository(
                new RequestAttributeSecurityContextRepository(),
                new HttpSessionSecurityContextRepository()
        );
    }

    // Note:
    // Spring Boot/Security will auto-configure a DaoAuthenticationProvider-backed AuthenticationManager
    // as long as a UserDetailsService and PasswordEncoder beans are present (which we have).
    // This avoids using deprecated DaoAuthenticationProvider constructors/setters directly.

    @Bean
    @Order(0)
    public SecurityFilterChain sessionSecurityFilterChain(
            HttpSecurity http,
            HandlerExceptionResolver handlerExceptionResolver
    ) throws Exception {
        // Session-based chain for /api/session/** endpoints
        http
                .securityMatcher("/api/session/**", "/h2-console/**", "/actuator/health")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(reg -> reg
                        .requestMatchers(
                                "/api/session/login",
                                "/h2-console/**",
                                "/actuator/health"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .headers(headers -> headers.frameOptions(frame -> frame.disable()))
        ;

        // Filters: exception delegation first (no JWT filter on session chain)
        http.addFilterBefore(new ExceptionHandlingFilter(handlerExceptionResolver), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain jwtSecurityFilterChain(
            HttpSecurity http,
            JwtProvider jwtProvider,
            RedisTokenService redisTokenService,
            HandlerExceptionResolver handlerExceptionResolver
    ) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(reg -> reg
                        .requestMatchers(
                                "/api/auth/**",
                                "/h2-console/**",
                                "/actuator/health"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .headers(headers -> headers.frameOptions(frame -> frame.disable()))
        ;

        // Filters: exception delegation first, then JWT auth
        http.addFilterBefore(new ExceptionHandlingFilter(handlerExceptionResolver), UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(new JwtAuthenticationFilter(jwtProvider, redisTokenService), ExceptionHandlingFilter.class);

        return http.build();
    }
}
