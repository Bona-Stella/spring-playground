package com.github.stella.springmsamq.worker;

import com.github.stella.springmsamq.common.exception.CommonExceptionConfig;
import com.github.stella.springmsamq.worker.config.WorkerProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@EnableConfigurationProperties(WorkerProperties.class)
@Import(CommonExceptionConfig.class)
public class WorkerApplication {
    public static void main(String[] args) {
        SpringApplication.run(WorkerApplication.class, args);
    }

    @RestController
    static class HealthController {
        @GetMapping("/health")
        public String health() { return "OK"; }
    }
}
