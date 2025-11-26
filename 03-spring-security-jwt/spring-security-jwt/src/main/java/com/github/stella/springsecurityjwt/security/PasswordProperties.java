package com.github.stella.springsecurityjwt.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.security.password")
public class PasswordProperties {
    /**
     * BCrypt work factor (aka strength). Default 10 if not configured.
     */
    private int bcryptStrength = 10;

    public int getBcryptStrength() {
        return bcryptStrength;
    }

    public void setBcryptStrength(int bcryptStrength) {
        this.bcryptStrength = bcryptStrength;
    }
}
