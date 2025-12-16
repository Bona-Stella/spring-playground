package com.github.stella.springmsamq.order.web;

import com.github.stella.springmsamq.common.ApiResponse;
import com.github.stella.springmsamq.order.domain.PurchaseOrder;
import com.github.stella.springmsamq.order.dto.CreateOrderRequest;
import com.github.stella.springmsamq.order.dto.OrderResponse;
import com.github.stella.springmsamq.order.service.OrderFacade;
import com.github.stella.springmsamq.order.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor // 생성자 자동 생성
public class OrderController {

    private final OrderFacade orderFacade; // Service 대신 Facade 주입

    @GetMapping("/ping")
    public ApiResponse<String> ping(HttpServletRequest request) {
        return ApiResponse.success("pong", request.getRequestURI());
    }

    @PostMapping
    public ApiResponse<OrderResponse> create(
            @Valid @RequestBody CreateOrderRequest req,
            HttpServletRequest request
    ) {
        // 1. 헤더에서 유저 ID 추출 (Gateway가 검증한 값)
        String userIdHeader = request.getHeader("X-User-Id");

        // 헤더가 없으면 401 또는 400 에러 처리 (Gateway를 안 거치고 들어온 경우 등)
        if (userIdHeader == null) {
            throw new IllegalArgumentException("Missing X-User-Id header");
        }
        Long userId = Long.parseLong(userIdHeader);

        // 2. Facade를 통해 주문 생성 (Lock -> Transaction -> Event)
        // Body에 있는 userId는 무시하거나 DTO에서 제거해야 합니다.
        PurchaseOrder order = orderFacade.createOrder(userId, req.productId(), req.quantity());

        // 3. 깔끔한 DTO로 반환
        return ApiResponse.created(
                OrderResponse.from(order),
                request.getRequestURI()
        );
    }
}
