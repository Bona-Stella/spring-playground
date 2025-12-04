package com.github.stella.springmsamq.order.web;

import com.github.stella.springmsamq.common.ApiResponse;
import com.github.stella.springmsamq.order.domain.Product;
import com.github.stella.springmsamq.order.service.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/order")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/products")
    public ApiResponse<List<Product>> list(HttpServletRequest request) {
        return ApiResponse.success(productService.findAll(), request.getRequestURI());
    }

    @GetMapping("/products/{id}")
    public ApiResponse<Product> get(@PathVariable Long id, HttpServletRequest request) {
        return ApiResponse.success(productService.findById(id), request.getRequestURI());
    }
}
