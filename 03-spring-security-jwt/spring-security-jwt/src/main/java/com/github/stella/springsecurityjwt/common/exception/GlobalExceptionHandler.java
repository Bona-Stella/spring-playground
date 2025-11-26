package com.github.stella.springsecurityjwt.common.exception;

import com.github.stella.springsecurityjwt.common.error.CustomException;
import com.github.stella.springsecurityjwt.common.error.ErrorCode;
import com.github.stella.springsecurityjwt.common.error.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException e, HttpServletRequest request) {
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity.status(errorCode.status())
                .body(ErrorResponse.of(errorCode, request.getRequestURI()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ErrorResponse> handleValidation(Exception e, HttpServletRequest request) {
        ErrorCode errorCode = ErrorCode.VALIDATION_ERROR;
        return ResponseEntity.status(errorCode.status())
                .body(ErrorResponse.of(errorCode, request.getRequestURI()));
    }

    @ExceptionHandler({AuthenticationException.class, BadCredentialsException.class})
    public ResponseEntity<ErrorResponse> handleAuth(AuthenticationException e, HttpServletRequest request) {
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;
        return ResponseEntity.status(errorCode.status())
                .body(ErrorResponse.of(errorCode, request.getRequestURI()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException e, HttpServletRequest request) {
        ErrorCode errorCode = ErrorCode.ACCESS_DENIED;
        return ResponseEntity.status(errorCode.status())
                .body(ErrorResponse.of(errorCode, request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e, HttpServletRequest request) {
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(errorCode.status())
                .body(ErrorResponse.of(errorCode, request.getRequestURI()));
    }
}
