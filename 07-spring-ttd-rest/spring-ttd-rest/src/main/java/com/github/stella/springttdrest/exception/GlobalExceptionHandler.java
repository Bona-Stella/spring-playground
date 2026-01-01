package com.github.stella.springttdrest.exception;

import com.github.stella.springttdrest.dto.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice // 모든 컨트롤러의 예외를 감시합니다.
public class GlobalExceptionHandler {

    // IllegalArgumentException이 발생하면 이 메서드가 잡습니다.
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {

        // 우리가 정의한 DTO로 변환해서 응답
        ErrorResponse response = new ErrorResponse("BAD_REQUEST", e.getMessage());

        return ResponseEntity.badRequest() // 400 Status Code
                .body(response);
    }
}