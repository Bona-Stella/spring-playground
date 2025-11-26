package com.github.stella.springsecurityjwt.auth;

import com.github.stella.springsecurityjwt.auth.dto.AuthDtos.LoginRequest;
import com.github.stella.springsecurityjwt.auth.dto.AuthDtos.RegisterRequest;
import com.github.stella.springsecurityjwt.auth.dto.AuthDtos.TokenResponse;
import com.github.stella.springsecurityjwt.common.error.CustomException;
import com.github.stella.springsecurityjwt.common.error.ErrorCode;
import com.github.stella.springsecurityjwt.security.RedisTokenService;
import com.github.stella.springsecurityjwt.security.jwt.JwtProperties;
import com.github.stella.springsecurityjwt.security.jwt.JwtProvider;
import com.github.stella.springsecurityjwt.security.jwt.TokenType;
import com.github.stella.springsecurityjwt.user.domain.User;
import com.github.stella.springsecurityjwt.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;
    private final JwtProperties props;
    private final RedisTokenService redisTokenService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtProvider jwtProvider,
                       JwtProperties props, RedisTokenService redisTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtProvider = jwtProvider;
        this.props = props;
        this.redisTokenService = redisTokenService;
    }

    public TokenResponse register(RegisterRequest req) {
        if (userRepository.existsByUsername(req.username())) {
            throw new CustomException(ErrorCode.DUPLICATE_RESOURCE);
        }
        // 주의: 여기서 User는 org.springframework...User가 아니라 도메인 Entity User여야 함
        User user = new User(req.username(), passwordEncoder.encode(req.password()), Set.of("USER"));
        userRepository.save(user);
        return issueTokens(user.getUsername(), List.copyOf(user.getRoles()));
    }

    public TokenResponse login(LoginRequest req) {
        // 1. 인증 시도
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.username(), req.password())
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        // 2. 유저 정보 조회 및 토큰 발급
        User user = userRepository.findByUsername(req.username())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return issueTokens(user.getUsername(), List.copyOf(user.getRoles()));
    }

    private TokenResponse issueTokens(String username, List<String> roles) {
        String access = jwtProvider.generateToken(username, roles, TokenType.ACCESS);
        String refresh = jwtProvider.generateToken(username, roles, TokenType.REFRESH);

        // Refresh Token 저장
        redisTokenService.storeRefreshToken(username, refresh, props.getRefreshTokenValiditySeconds());

        return new TokenResponse(access, refresh);
    }

    public TokenResponse refresh(String refreshToken) {
        // 1. 토큰 파싱 (여기서 만료/위변조 체크됨) -> Claims 재사용
        Claims claims = jwtProvider.parseClaims(refreshToken).getBody();

        // 2. Refresh Token 타입 확인
        if (!jwtProvider.isRefreshToken(claims)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        String username = claims.getSubject();

        // 3. Redis에 저장된 토큰과 일치하는지 확인
        String stored = redisTokenService.getRefreshToken(username).orElse(null);
        if (stored == null || !stored.equals(refreshToken)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        // 4. 기존 정보로 새 토큰 발급 (Rotate)
        List<String> roles = jwtProvider.getRoles(claims);
        return issueTokens(username, roles);
    }

    public void logout(String accessToken) {
        // 1. 토큰 파싱 -> Claims 재사용
        Claims claims = jwtProvider.parseClaims(accessToken).getBody();

        // 2. Access Token 타입 확인
        if (!jwtProvider.isAccessToken(claims)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        String username = claims.getSubject();

        // 3. 남은 만료 시간 계산 (Provider 재호출 없이 직접 계산)
        long expiration = claims.getExpiration().getTime();
        long now = System.currentTimeMillis();
        long remain = expiration - now;

        // 4. 블랙리스트 등록 및 Refresh Token 삭제
        if (remain > 0) {
            redisTokenService.blacklistAccessToken(accessToken, remain);
        }
        redisTokenService.deleteRefreshToken(username);
    }
}
