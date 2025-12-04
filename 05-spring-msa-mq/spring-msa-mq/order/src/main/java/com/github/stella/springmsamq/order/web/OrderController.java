package com.github.stella.springmsamq.order.web;

import com.github.stella.springmsamq.common.ApiResponse;
import com.github.stella.springmsamq.order.domain.PurchaseOrder;
import com.github.stella.springmsamq.order.dto.CreateOrderRequest;
import com.github.stella.springmsamq.order.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/order")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/ping")
    public ApiResponse<String> ping(HttpServletRequest request) {
        return ApiResponse.success("pong", request.getRequestURI());
    }

    @PostMapping
    public ApiResponse<?> create(@Valid @RequestBody CreateOrderRequest req, HttpServletRequest request) {
        PurchaseOrder order = orderService.create(req.userId(), req.productId(), req.quantity());
        return ApiResponse.created(
                java.util.Map.of(
                        "orderId", order.getId(),
                        "userId", order.getUserId(),
                        "productId", order.getProductId(),
                        "quantity", order.getQuantity(),
                        "totalPrice", order.getTotalPrice()
                ),
                request.getRequestURI()
        );
    }
}
