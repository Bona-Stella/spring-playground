package com.github.stella.springsecurityjwt.hello;

import com.github.stella.springsecurityjwt.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class HelloController {

    @GetMapping("/hello")
    public ApiResponse<Map<String, Object>> hello(Authentication authentication, HttpServletRequest request) {
        String user = authentication == null ? "anonymous" : authentication.getName();
        Map<String, Object> data = Map.of(
                "greeting", "Hello, "+ user + "!",
                "user", user
        );
        return ApiResponse.success(data, request.getRequestURI());
    }
}
