package com.github.stella.springsecurityjwt.security.filter;

import com.github.stella.springsecurityjwt.common.error.CustomException;
import com.github.stella.springsecurityjwt.common.error.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

/**
 * Filter/인증 단계에서 발생하는 모든 예외를 Spring MVC 예외 흐름으로 위임해
 * @RestControllerAdvice(GlobalExceptionHandler)가 통일된 에러 응답을 반환하도록 한다.
 */
public class ExceptionHandlingFilter extends OncePerRequestFilter {
    private final HandlerExceptionResolver resolver;

    public ExceptionHandlingFilter(HandlerExceptionResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (CustomException | AuthenticationException | AccessDeniedException e) {
            resolver.resolveException(request, response, null, e);
        } catch (Exception e) {
            resolver.resolveException(request, response, null, new CustomException(ErrorCode.INTERNAL_SERVER_ERROR));
        }
    }
}
