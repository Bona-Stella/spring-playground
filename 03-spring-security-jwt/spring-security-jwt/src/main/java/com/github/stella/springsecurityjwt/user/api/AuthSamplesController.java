package com.github.stella.springsecurityjwt.user.api;

import com.github.stella.springsecurityjwt.common.api.ApiResponse;
import com.github.stella.springsecurityjwt.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 다양한 방식의 인증 정보 주입(파라미터) 사용 예제 컨트롤러.
 * 기존 코드 리팩터링 없이 참고용으로만 추가되었습니다.
 */
@RestController
@RequestMapping("/api/samples/auth")
public class AuthSamplesController {

    public record PrincipalDto(Long userId, String username, List<String> roles) {}

    // 1) Authentication 파라미터 주입 예시
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/authentication")
    public ResponseEntity<ApiResponse<PrincipalDto>> withAuthentication(Authentication authentication,
                                                                        HttpServletRequest request) {
        String username = authentication.getName();
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
                .collect(Collectors.toList());

        Long userId = null;
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails cud) {
            userId = cud.getId();
        }
        return ResponseEntity.ok(ApiResponse.success(new PrincipalDto(userId, username, roles), request.getRequestURI()));
    }

    // 2) java.security.Principal 주입 예시 (username만 필요할 때 간단하게)
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/principal")
    public ResponseEntity<ApiResponse<PrincipalDto>> withPrincipal(Principal principal, HttpServletRequest request) {
        String username = principal.getName();
        // 역할/ID가 필요하면 Authentication을 함께 받거나 SecurityContext에서 조회
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
                .collect(Collectors.toList());
        Long userId = (authentication.getPrincipal() instanceof CustomUserDetails cud) ? cud.getId() : null;
        return ResponseEntity.ok(ApiResponse.success(new PrincipalDto(userId, username, roles), request.getRequestURI()));
    }

    // 3) @AuthenticationPrincipal로 커스텀 Principal 직접 주입
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/authentication-principal")
    public ResponseEntity<ApiResponse<PrincipalDto>> withAuthenticationPrincipal(
            @AuthenticationPrincipal CustomUserDetails principal,
            HttpServletRequest request) {
        String username = principal.getUsername();
        List<String> roles = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
                .collect(Collectors.toList());
        Long userId = principal.getId();
        return ResponseEntity.ok(ApiResponse.success(new PrincipalDto(userId, username, roles), request.getRequestURI()));
    }

    // 4) @AuthenticationPrincipal + SpEL expression으로 특정 필드만 주입
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/authentication-principal/id")
    public ResponseEntity<ApiResponse<Long>> withAuthenticationPrincipalId(
            @AuthenticationPrincipal(expression = "id") Long userId,
            HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(userId, request.getRequestURI()));
    }

    // 5) @CurrentSecurityContext 사용 예시 (필요 시 SecurityContext/Authentication 꺼내쓰기)
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/current-context")
    public ResponseEntity<ApiResponse<PrincipalDto>> withCurrentSecurityContext(
            @CurrentSecurityContext SecurityContext context,
            HttpServletRequest request) {
        Authentication authentication = context.getAuthentication();
        String username = authentication.getName();
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
                .collect(Collectors.toList());
        Long userId = (authentication.getPrincipal() instanceof CustomUserDetails cud) ? cud.getId() : null;
        return ResponseEntity.ok(ApiResponse.success(new PrincipalDto(userId, username, roles), request.getRequestURI()));
    }
}
