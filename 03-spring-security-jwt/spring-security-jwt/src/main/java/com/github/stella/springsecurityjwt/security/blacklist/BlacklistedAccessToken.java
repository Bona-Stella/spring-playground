package com.github.stella.springsecurityjwt.security.blacklist;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "blacklisted_access_tokens", indexes = {
        @Index(name = "idx_blacklist_token", columnList = "token", unique = true),
        @Index(name = "idx_blacklist_exp", columnList = "expiresAt")
})
public class BlacklistedAccessToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 512)
    private String token;

    @Column(nullable = false)
    private Instant expiresAt;

    protected BlacklistedAccessToken() {}

    public BlacklistedAccessToken(String token, Instant expiresAt) {
        this.token = token;
        this.expiresAt = expiresAt;
    }

    public Long getId() { return id; }
    public String getToken() { return token; }
    public Instant getExpiresAt() { return expiresAt; }
}
