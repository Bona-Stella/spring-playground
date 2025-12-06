package com.github.stella.springmsamq.gateway.config;

import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ServerCodecConfigurer;

@Configuration
public class ErrorHandlerConfig {

    // WebFlux용 기본 ErrorAttributes 빈 등록
    @Bean
    public ErrorAttributes errorAttributes() {
        return new DefaultErrorAttributes();
    }

    // WebProperties(resources 등) 빈 등록 (AbstractErrorWebExceptionHandler에 필요)
    @Bean
    public WebProperties webProperties() {
        return new WebProperties();
    }

    // ServerCodecConfigurer가 자동 주입되지 않는 환경을 대비한 기본 빈
    @Bean
    public ServerCodecConfigurer serverCodecConfigurer() {
        return ServerCodecConfigurer.create();
    }
}
