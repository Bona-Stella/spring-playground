package com.github.stella.springsecurityjwt.user;

import com.github.stella.springsecurityjwt.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class MeController {

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me(Authentication authentication, HttpServletRequest request) {
        String user = authentication == null ? "anonymous" : authentication.getName();
        List<String> roles = authentication == null ? List.of() : authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        Map<String, Object> data = Map.of(
                "username", user,
                "roles", roles
        );
        return ApiResponse.success(data, request.getRequestURI());
    }
}
