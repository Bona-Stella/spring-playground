package com.github.stella.springsecurityjwt.auth;

import com.github.stella.springsecurityjwt.auth.dto.AuthDtos;
import com.github.stella.springsecurityjwt.common.api.ApiResponse;
import com.github.stella.springsecurityjwt.security.CustomUserDetails;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 세션 기반 로그인 API (JWT와 병행 동작). 경로: /api/session/**
 */
@RestController
@RequestMapping("/api/session")
public class SessionAuthController {

    private final AuthenticationManager authenticationManager;

    // 1. 컨텍스트 저장소 (세션 처리용) - 매번 new 하지 않고 재사용
    private final SecurityContextRepository securityContextRepository;
    // 2. 컨텍스트 전략 (Holder 제어용) - 정적 호출 대신 전략 객체 사용
    private final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();
    // 3. 로그아웃 핸들러를 필드로 선언하여 재사용 (불필요한 객체 생성 방지)
    private final SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();

    public SessionAuthController(AuthenticationManager authenticationManager, SecurityContextRepository securityContextRepository) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    public record SessionLoginResponse(Long userId, String username, List<String> roles) {
    }

    /**
     * 세션 로그인: 성공 시 서버 세션(JSESSIONID) 생성 및 저장.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<SessionLoginResponse>> login(@Validated @RequestBody AuthDtos.LoginRequest req,
                                                                   HttpServletRequest request,
                                                                   HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.username(), req.password())
        );

        // SecurityContext에 저장 후 세션으로 영속화
        // SecurityContext context = SecurityContextHolder.createEmptyContext();
        // SecurityContextHolder.setContext(context);
        SecurityContext context = securityContextHolderStrategy.createEmptyContext();
        context.setAuthentication(authentication);
        securityContextHolderStrategy.setContext(context);

        // 세션 생성 및 컨텍스트 저장
        // HttpSession session = request.getSession(true);
        // new HttpSessionSecurityContextRepository().saveContext(context, request, response);
        securityContextRepository.saveContext(context, request, response);

        String username = authentication.getName();
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
                .collect(Collectors.toList());

        Long userId = (authentication.getPrincipal() instanceof CustomUserDetails cud) ? cud.getId() : null;

        return ResponseEntity.ok(ApiResponse.success(new SessionLoginResponse(userId, username, roles), request.getRequestURI()));
    }

    /**
     * 현재 세션 사용자 정보
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<SessionLoginResponse>> me(@AuthenticationPrincipal CustomUserDetails user,
                                                                HttpServletRequest request) {
        /*
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
                .collect(Collectors.toList());
        Long userId = (authentication.getPrincipal() instanceof CustomUserDetails cud) ? cud.getId() : null;
        return ResponseEntity.ok(ApiResponse.success(new SessionLoginResponse(userId, username, roles), request.getRequestURI()));
       */
        List<String> roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(new SessionLoginResponse(user.getId(), user.getUsername(), roles), request.getRequestURI()));
    }

    /**
     * 세션 로그아웃: 세션 무효화 및 컨텍스트 정리
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(Authentication authentication,
                                                      HttpServletRequest request,
                                                      HttpServletResponse response) {
        // new SecurityContextLogoutHandler().logout(request, response, SecurityContextHolder.getContext().getAuthentication());
        // return ResponseEntity.ok(ApiResponse.success("LOGOUT_OK", request.getRequestURI()));
        this.logoutHandler.logout(request, response, authentication);
        // request.logout(); <- 같은 역할
        return ResponseEntity.ok(ApiResponse.success("LOGOUT_OK", request.getRequestURI()));
    }
}
