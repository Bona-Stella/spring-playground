package com.github.stella.springmsamq.order.web;

import com.github.stella.springmsamq.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    @GetMapping("/ping")
    public ApiResponse<String> ping(HttpServletRequest request) {
        return ApiResponse.success("pong", request.getRequestURI());
    }
}
