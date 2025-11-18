package com.github.stella.springapiboard.common.web;

import com.github.stella.springapiboard.common.error.CustomException;
import com.github.stella.springapiboard.common.error.ErrorCode;
import com.github.stella.springapiboard.common.error.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

/*    private boolean isSwaggerPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/v3/api-docs") ||
               path.startsWith("/swagger-ui") ||
               path.equals("/swagger-ui.html");
    }*/

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException e, HttpServletRequest request) {
        var errorCode = e.getErrorCode();
        return ResponseEntity.status(errorCode.status())
                .body(ErrorResponse.of(errorCode, request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e, HttpServletRequest request) {
        // 메시지에 어떤 필드가 실패했는지 간단히 첨부
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ":" + (fe.getDefaultMessage() == null ? "invalid" : fe.getDefaultMessage()))
                .collect(Collectors.joining(", "));
        var code = ErrorCode.VALIDATION_ERROR;
        // 기본 ErrorResponse 메시지 규약을 따르되, 상세는 로깅/추가 확장 대상
        return ResponseEntity.status(code.status())
                .body(ErrorResponse.of(code, request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e, HttpServletRequest request) {
        // Swagger 관련 경로는 처리하지 않음 (Springdoc이 자체 처리하도록)
        /*if (isSwaggerPath(request)) {
            throw new RuntimeException(e);
        }*/

        var code = ErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(code.status())
                .body(ErrorResponse.of(code, request.getRequestURI()));
    }
}
