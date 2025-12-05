package com.github.stella.springmsamq.chat;

import com.github.stella.springmsamq.common.exception.CommonExceptionConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@Import(CommonExceptionConfig.class)
public class ChatApplication {
    public static void main(String[] args) {
        SpringApplication.run(ChatApplication.class, args);
    }

    @RestController
    static class HealthController {
        @GetMapping("/health")
        public String health() { return "OK"; }
    }
}
