package com.github.stella.springsecurityjwt.user.api;

import com.github.stella.springsecurityjwt.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class MeController {

    public record MeDto(String username, List<String> roles) {}

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MeDto>> me(Authentication authentication, HttpServletRequest request) {
        String username = authentication.getName();
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(new MeDto(username, roles), request.getRequestURI()));
    }
}
