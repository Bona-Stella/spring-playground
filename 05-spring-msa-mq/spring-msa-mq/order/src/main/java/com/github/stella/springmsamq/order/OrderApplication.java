package com.github.stella.springmsamq.order;

import com.github.stella.springmsamq.common.exception.CommonExceptionConfig;
import com.github.stella.springmsamq.order.domain.Product;
import com.github.stella.springmsamq.order.repo.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@EnableCaching
@SpringBootApplication
@Import(CommonExceptionConfig.class)
public class OrderApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }

    @RestController
    static class HealthController {
        @GetMapping("/health")
        public String health() { return "OK"; }
    }

    @Bean
    CommandLineRunner seedProducts(ProductRepository repo) {
        return args -> {
            if (repo.count() == 0) {
                repo.save(new Product(null, "Apple", 1000, 50));
                repo.save(new Product(null, "Banana", 500, 100));
                repo.save(new Product(null, "Orange", 800, 80));
            }
        };
    }
}
