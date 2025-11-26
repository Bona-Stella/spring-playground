package com.github.stella.springsecurityjwt.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
    private String secret;
    private long accessTokenValiditySeconds;
    private long refreshTokenValiditySeconds;

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public long getAccessTokenValiditySeconds() { return accessTokenValiditySeconds; }
    public void setAccessTokenValiditySeconds(long accessTokenValiditySeconds) { this.accessTokenValiditySeconds = accessTokenValiditySeconds; }
    public long getRefreshTokenValiditySeconds() { return refreshTokenValiditySeconds; }
    public void setRefreshTokenValiditySeconds(long refreshTokenValiditySeconds) { this.refreshTokenValiditySeconds = refreshTokenValiditySeconds; }
}
