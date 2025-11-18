package com.github.stella.springsecurityjwt.auth.token;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    List<RefreshToken> findAllByUsernameAndRevokedIsFalse(String username);
    long deleteByExpiresAtBefore(Instant time);
}
