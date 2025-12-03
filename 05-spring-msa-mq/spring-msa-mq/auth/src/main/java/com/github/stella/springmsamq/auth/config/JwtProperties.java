package com.github.stella.springmsamq.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private long accessTokenValiditySeconds;
    private long refreshTokenValiditySeconds;
    private String issuer;
    private String privateKeyLocation;
    private String publicKeyLocation;

    public long getAccessTokenValiditySeconds() { return accessTokenValiditySeconds; }
    public void setAccessTokenValiditySeconds(long accessTokenValiditySeconds) { this.accessTokenValiditySeconds = accessTokenValiditySeconds; }

    public long getRefreshTokenValiditySeconds() { return refreshTokenValiditySeconds; }
    public void setRefreshTokenValiditySeconds(long refreshTokenValiditySeconds) { this.refreshTokenValiditySeconds = refreshTokenValiditySeconds; }

    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }

    public String getPrivateKeyLocation() { return privateKeyLocation; }
    public void setPrivateKeyLocation(String privateKeyLocation) { this.privateKeyLocation = privateKeyLocation; }

    public String getPublicKeyLocation() { return publicKeyLocation; }
    public void setPublicKeyLocation(String publicKeyLocation) { this.publicKeyLocation = publicKeyLocation; }
}
