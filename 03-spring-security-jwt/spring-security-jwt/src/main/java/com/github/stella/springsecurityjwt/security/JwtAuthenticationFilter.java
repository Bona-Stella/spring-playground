package com.github.stella.springsecurityjwt.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.github.stella.springsecurityjwt.security.blacklist.BlacklistedAccessTokenRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final BlacklistedAccessTokenRepository blacklistRepository;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider,
                                   BlacklistedAccessTokenRepository blacklistRepository) {
        this.tokenProvider = tokenProvider;
        this.blacklistRepository = blacklistRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String bearer = request.getHeader(HttpHeaders.AUTHORIZATION);
        String token = resolveToken(bearer);

        if (token != null) {
            try {
                Claims claims = tokenProvider.parseClaims(token);
                // Only accept ACCESS tokens for authentication
                if (!Objects.equals(claims.get("type"), JwtTokenProvider.TokenType.ACCESS.name())) {
                    filterChain.doFilter(request, response);
                    return;
                }
                // check blacklist
                if (blacklistRepository.findByToken(token).isPresent()) {
                    filterChain.doFilter(request, response);
                    return;
                }
                String subject = claims.getSubject();
                @SuppressWarnings("unchecked")
                List<String> roles = (List<String>) claims.get("roles");
                List<GrantedAuthority> authorities = roles == null ? List.of() : roles.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(subject, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception ignored) {
                // 토큰이 잘못되었거나 만료된 경우: 인증 없이 계속 진행 (컨트롤러/시큐리티가 차단)
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(String bearer) {
        if (bearer == null || bearer.isBlank()) return null;
        if (bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
