package com.github.stella.springsecurityjwt.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/**
 * 애플리케이션 전역에서 사용할 커스텀 Principal 구현체.
 * - 컨트롤러/서비스에서 authentication.getPrincipal() 캐스팅 후 id 등 추가 필드에 접근 가능.
 */
public class CustomUserDetails implements UserDetails {
    private final Long id;
    private final String username;
    private final String password; // JWT 인증 경로에서는 "" 사용
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(Long id, String username, String password,
                             Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.authorities = authorities;
    }

    public Long getId() { return id; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }

    @Override
    public String getPassword() { return password; }

    @Override
    public String getUsername() { return username; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}
