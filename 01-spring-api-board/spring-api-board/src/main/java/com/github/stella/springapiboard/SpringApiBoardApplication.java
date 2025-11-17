package com.github.stella.springapiboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class SpringApiBoardApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringApiBoardApplication.class, args);
    }

}
