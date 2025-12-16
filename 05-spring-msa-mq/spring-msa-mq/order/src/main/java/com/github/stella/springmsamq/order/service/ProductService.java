package com.github.stella.springmsamq.order.service;

import com.github.stella.springmsamq.order.domain.Product;
import com.github.stella.springmsamq.order.repo.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    // 오타 방지를 위해 상수는 유지하되, 관례상 대문자로만 쓰는 게 좋습니다.
    public static final String CACHE_PRODUCTS = "products";
    public static final String CACHE_PRODUCT = "product";

    private final ProductRepository productRepository;

    @Cacheable(cacheNames = CACHE_PRODUCTS)
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    @Cacheable(cacheNames = CACHE_PRODUCT, key = "#id")
    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + id));
    }

    /**
     * 재고 차감 (주문 시 호출)
     * [성능 개선] allEntries = true 제거 -> key = "#productId" 로 변경
     * 이제 "사과" 재고가 바뀌면 "사과" 캐시만 지워집니다.
     */
    @Transactional
    @CacheEvict(cacheNames = CACHE_PRODUCT, key = "#productId")
    public void decrementStock(Long productId, int quantity) {
        Product p = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + productId));

        if (p.getStock() < quantity) {
            // 로그를 남겨두면 나중에 재고 부족 에러 추적하기 좋습니다.
            log.warn("재고 부족 발생 - productId: {}, 요청: {}, 현재: {}", productId, quantity, p.getStock());
            throw new IllegalStateException("Insufficient stock");
        }

        p.setStock(p.getStock() - quantity);
        // JPA Dirty Checking으로 save() 없어도 업데이트되지만, 명시적으로 적어도 됩니다.
        // productRepository.save(p);
    }

    /**
     * 재고 복원 (주문 취소/보상 트랜잭션 시 호출)
     */
    @Transactional
    @CacheEvict(cacheNames = CACHE_PRODUCT, key = "#productId")
    public void incrementStock(Long productId, int quantity) {
        Product p = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + productId));

        p.setStock(p.getStock() + quantity);
    }
}
