package com.github.stella.springsecurityjwt.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "app.security.password")
public class PasswordProperties {
    /**
     * BCrypt work factor (aka strength). Default 10 if not configured.
     */
    private int bcryptStrength = 10;

}
