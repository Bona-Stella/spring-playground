package com.github.stella.springsecurityjwt.auth;

import com.github.stella.springsecurityjwt.auth.dto.LoginRequest;
import com.github.stella.springsecurityjwt.auth.dto.RefreshRequest;
import com.github.stella.springsecurityjwt.auth.dto.SignupRequest;
import com.github.stella.springsecurityjwt.auth.dto.TokenResponse;
import com.github.stella.springsecurityjwt.common.error.CustomException;
import com.github.stella.springsecurityjwt.common.error.ErrorCode;
import com.github.stella.springsecurityjwt.auth.token.RefreshToken;
import com.github.stella.springsecurityjwt.auth.token.RefreshTokenRepository;
import com.github.stella.springsecurityjwt.security.JwtTokenProvider;
import com.github.stella.springsecurityjwt.security.blacklist.BlacklistedAccessToken;
import com.github.stella.springsecurityjwt.security.blacklist.BlacklistedAccessTokenRepository;
import com.github.stella.springsecurityjwt.user.Role;
import com.github.stella.springsecurityjwt.user.User;
import com.github.stella.springsecurityjwt.user.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final BlacklistedAccessTokenRepository blacklistedAccessTokenRepository;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtTokenProvider tokenProvider,
                       RefreshTokenRepository refreshTokenRepository,
                       BlacklistedAccessTokenRepository blacklistedAccessTokenRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.refreshTokenRepository = refreshTokenRepository;
        this.blacklistedAccessTokenRepository = blacklistedAccessTokenRepository;
    }

    @Transactional
    public void signup(SignupRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new CustomException(ErrorCode.DUPLICATE_RESOURCE);
        }
        User user = new User(
                request.username(),
                passwordEncoder.encode(request.password()),
                Set.of(Role.ROLE_USER)
        );
        userRepository.save(user);
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );
        String username = authentication.getName();

        List<String> roles = authentication.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .toList();

        String access = tokenProvider.createToken(username, roles, JwtTokenProvider.TokenType.ACCESS);
        String refresh = tokenProvider.createToken(username, roles, JwtTokenProvider.TokenType.REFRESH);
        // persist refresh token
        refreshTokenRepository.save(new RefreshToken(
                username,
                refresh,
                tokenProvider.getExpiration(refresh)
        ));
        return new TokenResponse(access, refresh);
    }

    public TokenResponse refresh(RefreshRequest request) {
        String token = request.refreshToken();
        if (!tokenProvider.isRefreshToken(token)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
        RefreshToken stored = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_TOKEN));
        if (stored.isRevoked()) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
        if (stored.getExpiresAt().isBefore(java.time.Instant.now())) {
            throw new CustomException(ErrorCode.EXPIRED_TOKEN);
        }

        String subject = tokenProvider.getSubject(token);
        List<String> roles = tokenProvider.getRoles(token);
        String newAccess = tokenProvider.createToken(subject, roles, JwtTokenProvider.TokenType.ACCESS);
        String newRefresh = tokenProvider.createToken(subject, roles, JwtTokenProvider.TokenType.REFRESH);

        // rotate refresh token: revoke old, save new
        stored.revoke(newRefresh);
        refreshTokenRepository.save(stored);
        refreshTokenRepository.save(new RefreshToken(subject, newRefresh, tokenProvider.getExpiration(newRefresh)));

        return new TokenResponse(newAccess, newRefresh);
    }

    @Transactional
    public void logout(String username, String accessToken) {
        // revoke all active refresh tokens for user
        refreshTokenRepository.findAllByUsernameAndRevokedIsFalse(username).forEach(rt -> {
            rt.revoke("LOGOUT");
            refreshTokenRepository.save(rt);
        });
        // blacklist current access token (if provided)
        if (accessToken != null && !accessToken.isBlank() && tokenProvider.isAccessToken(accessToken)) {
            java.time.Instant exp = tokenProvider.getExpiration(accessToken);
            if (exp != null && exp.isAfter(java.time.Instant.now())) {
                blacklistedAccessTokenRepository.save(new BlacklistedAccessToken(accessToken, exp));
            }
        }
    }
}
