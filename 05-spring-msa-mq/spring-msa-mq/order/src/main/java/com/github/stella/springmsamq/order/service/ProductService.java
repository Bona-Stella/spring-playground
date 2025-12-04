package com.github.stella.springmsamq.order.service;

import com.github.stella.springmsamq.order.domain.Product;
import com.github.stella.springmsamq.order.repo.ProductRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {
    public static final String CACHE_PRODUCTS = "products";
    public static final String CACHE_PRODUCT = "product";

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Cacheable(cacheNames = CACHE_PRODUCTS)
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    @Cacheable(cacheNames = CACHE_PRODUCT, key = "#id")
    public Product findById(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Product not found"));
    }

    @Transactional
    @CacheEvict(cacheNames = {CACHE_PRODUCTS, CACHE_PRODUCT}, allEntries = true)
    public void decrementStock(Long productId, int quantity) {
        Product p = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        if (p.getStock() < quantity) {
            throw new IllegalStateException("Insufficient stock");
        }
        p.setStock(p.getStock() - quantity);
        productRepository.save(p);
    }

    @Transactional
    @CacheEvict(cacheNames = {CACHE_PRODUCTS, CACHE_PRODUCT}, allEntries = true)
    public void incrementStock(Long productId, int quantity) {
        Product p = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        p.setStock(p.getStock() + quantity);
        productRepository.save(p);
    }
}
