package com.github.stella.springsecurityjwt.security.blacklist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface BlacklistedAccessTokenRepository extends JpaRepository<BlacklistedAccessToken, Long> {
    Optional<BlacklistedAccessToken> findByToken(String token);
    long deleteByExpiresAtBefore(Instant time);
}
