package com.github.stella.springmsamq.gateway.config;

import com.github.stella.springmsamq.gateway.security.InMemoryDenyList;
import com.github.stella.springmsamq.gateway.util.PemUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

import java.security.interfaces.RSAPublicKey;

@Configuration
public class JwtConfig {

    @Bean
    public ReactiveJwtDecoder  jwtDecoder(InMemoryDenyList denyList) throws Exception {
        // 기존 application.yml의 public-key-location 설정 대신 직접 로드하여 Validator를 주입한다.
        ClassPathResource pub = new ClassPathResource("keys/public.pem");
        RSAPublicKey publicKey = (RSAPublicKey) PemUtils.readPublicKey(pub.getInputStream());
        // 2. Reactive 디코더 생성 (NimbusReactiveJwtDecoder 사용)
        // NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(publicKey).build();
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withPublicKey(publicKey).build();


        OAuth2TokenValidator<Jwt> withTimestamp = JwtValidators.createDefault();
        OAuth2TokenValidator<Jwt> notRevoked = jwt -> {
            String jti = jwt.getId();
            if (jti != null && denyList.isDenied(jti)) {
                return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Token is revoked", null));
            }
            return OAuth2TokenValidatorResult.success();
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withTimestamp, notRevoked));
        return decoder;
    }
}
