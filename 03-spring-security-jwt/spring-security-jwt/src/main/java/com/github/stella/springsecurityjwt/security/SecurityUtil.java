package com.github.stella.springsecurityjwt.security;

import com.github.stella.springsecurityjwt.common.error.CustomException;
import com.github.stella.springsecurityjwt.common.error.ErrorCode;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * SecurityContext에서 현재 인증 정보를 편리하게 꺼내 쓰기 위한 유틸리티.
 * 실무에서 자주 쓰는 패턴을 모아두었습니다.
 */
public final class SecurityUtil {

    private SecurityUtil() {}

    /** 현재 스레드의 Authentication 반환 (없으면 null) */
    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /** 인증 여부 (익명/널 제외) */
    public static boolean isAuthenticated() {
        Authentication auth = getAuthentication();
        return auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
    }

    /** 익명 여부 */
    public static boolean isAnonymous() {
        return !isAuthenticated();
    }

    /** 현재 사용자 이름 (없으면 null) */
    public static String getUsername() {
        Authentication auth = getAuthentication();
        return auth != null ? auth.getName() : null;
    }

    /** 현재 사용자 ID (CustomUserDetails에 한함, 없으면 null) */
    public static Long getUserId() {
        CustomUserDetails cud = getCustomPrincipalOrNull();
        return cud != null ? cud.getId() : null;
    }

    /** 현재 사용자 ID가 반드시 있어야 할 때 사용 (없으면 UNAUTHORIZED 예외) */
    public static Long requireUserId() {
        Long id = getUserId();
        if (id == null) throw new CustomException(ErrorCode.UNAUTHORIZED);
        return id;
    }

    /** 현재 사용자 권한(문자열) 목록 반환 (ROLE_ 접두 포함) */
    public static List<String> getAuthorities() {
        Authentication auth = getAuthentication();
        if (auth == null) return List.of();
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        if (authorities == null) return List.of();
        return authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList());
    }

    /** 현재 사용자 역할 목록 반환 (ROLE_ 접두 제거하여 반환) */
    public static List<String> getRoles() {
        return getAuthorities().stream()
                .filter(Objects::nonNull)
                .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
                .collect(Collectors.toList());
    }

    /** 역할 보유 여부 (입력은 접두사 없이 전달: e.g., "ADMIN") */
    public static boolean hasRole(String role) {
        String target = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return getAuthorities().contains(target);
    }

    /** 여러 역할 중 하나라도 보유 여부 */
    public static boolean hasAnyRole(String... roles) {
        if (roles == null || roles.length == 0) return false;
        List<String> auths = getAuthorities();
        for (String role : roles) {
            String target = role.startsWith("ROLE_") ? role : "ROLE_" + role;
            if (auths.contains(target)) return true;
        }
        return false;
    }

    /** 현재 Principal을 CustomUserDetails로 안전 캐스팅 (실패 시 null) */
    public static CustomUserDetails getCustomPrincipalOrNull() {
        Authentication auth = getAuthentication();
        if (auth == null) return null;
        Object principal = auth.getPrincipal();
        return (principal instanceof CustomUserDetails) ? (CustomUserDetails) principal : null;
    }
}
