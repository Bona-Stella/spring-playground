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
import org.springframework.web.servlet.HandlerExceptionResolver;

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

    // Note:
    // Spring Boot/Security will auto-configure a DaoAuthenticationProvider-backed AuthenticationManager
    // as long as a UserDetailsService and PasswordEncoder beans are present (which we have).
    // This avoids using deprecated DaoAuthenticationProvider constructors/setters directly.

    @Bean
    public SecurityFilterChain securityFilterChain(
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
