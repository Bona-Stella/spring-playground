package com.github.stella.springmsamq.common.exception;

import com.github.stella.springmsamq.common.ErrorCode;
import com.github.stella.springmsamq.common.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CommonExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(e -> e.getField() + " " + e.getDefaultMessage())
                .orElse(ErrorCode.VALIDATION_ERROR.message());
        ErrorResponse body = new ErrorResponse(false, ErrorCode.VALIDATION_ERROR.status(),
                ErrorCode.VALIDATION_ERROR.name(), message,
                java.time.ZonedDateTime.now().toString(), request.getRequestURI());
        return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.status()).body(body);
    }

    @ExceptionHandler({ConstraintViolationException.class, IllegalArgumentException.class, IllegalStateException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception ex, HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.of(ErrorCode.INVALID_INPUT, request.getRequestURI());
        return ResponseEntity.status(ErrorCode.INVALID_INPUT.status()).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAny(Exception ex, HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR, request.getRequestURI());
        return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.status()).body(body);
    }
}
