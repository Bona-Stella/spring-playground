package com.github.stella.springsecurityjwt.admin;

import com.github.stella.springsecurityjwt.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ApiResponse<Map<String, Object>> admin(HttpServletRequest request) {
        return ApiResponse.success(Map.of(
                "message", "Admin area: only ADMIN can see this"
        ), request.getRequestURI());
    }
}
