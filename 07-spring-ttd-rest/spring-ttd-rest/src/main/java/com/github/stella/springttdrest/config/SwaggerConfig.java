package com.github.stella.springttdrest.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        String jwtSchemeName = "JWT Authentication";

        // 1. 보안 스키마 정의 (Header에 Bearer Token을 넣겠다)
        SecurityScheme securityScheme = new SecurityScheme()
                .name(jwtSchemeName)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");

        // 2. 보안 요구사항 정의 (이 API는 위 스키마를 쓴다)
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(jwtSchemeName);

        // 3. OpenAPI 객체 빌드
        return new OpenAPI()
                .info(new Info()
                        .title("포인트 시스템 API")
                        .description("TDD와 Spring Boot로 만든 포인트 충전/사용/조회 API 문서입니다.")
                        .version("1.0.0"))
                .addSecurityItem(securityRequirement)
                .components(new Components().addSecuritySchemes(jwtSchemeName, securityScheme));
    }
}