package com.github.stella.springmsamq.auth;

import com.github.stella.springmsamq.auth.config.JwtProperties;
import com.github.stella.springmsamq.common.exception.CommonExceptionConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
//@EnableConfigurationProperties(JwtProperties.class)
@Import(CommonExceptionConfig.class)
@ConfigurationPropertiesScan
public class AuthApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }

    @RestController
    static class HealthController {
        @GetMapping("/health")
        public String health() { return "OK"; }
    }
}
