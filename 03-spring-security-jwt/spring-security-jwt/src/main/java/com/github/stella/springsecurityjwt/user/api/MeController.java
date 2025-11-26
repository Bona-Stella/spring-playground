package com.github.stella.springsecurityjwt.user.api;

import com.github.stella.springsecurityjwt.common.api.ApiResponse;
import com.github.stella.springsecurityjwt.security.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class MeController {

    public record MeDto(String username, List<String> roles) {}

    // USER 또는 ADMIN 권한이 있어야 접근 가능 (ROLE_ 접두사 없이 작성)
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MeDto>> me(HttpServletRequest request) {
        String username = SecurityUtil.getUsername();
        List<String> roles = SecurityUtil.getRoles();
        return ResponseEntity.ok(ApiResponse.success(new MeDto(username, roles), request.getRequestURI()));
    }

    // 인증만 되어 있으면 접근 가능. 현재 로그인한 사용자의 userId 반환 예시
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me/id")
    public ResponseEntity<ApiResponse<Long>> myId(HttpServletRequest request) {
        Long userId = SecurityUtil.requireUserId();
        return ResponseEntity.ok(ApiResponse.success(userId, request.getRequestURI()));
    }
}
