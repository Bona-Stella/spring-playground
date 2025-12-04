package com.github.stella.springmsamq.order.repo;

import com.github.stella.springmsamq.order.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
