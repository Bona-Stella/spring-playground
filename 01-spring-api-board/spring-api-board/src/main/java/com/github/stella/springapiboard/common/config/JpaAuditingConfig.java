package com.github.stella.springapiboard.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

// 설정파일 선언
@Configuration
// Auditing 기능 활성화 (스위치 On)
@EnableJpaAuditing
public class JpaAuditingConfig {
}
