package com.github.stella.springmsamq.demo;

import com.github.stella.springmsamq.common.ApiResponse;
import com.github.stella.springmsamq.common.CustomException;
import com.github.stella.springmsamq.common.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/demo")
@Validated
public class DemoController {

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> get(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        Map<String, Object> data = Map.of("id", id, "title", "데모 데이터");
        return ResponseEntity.ok(ApiResponse.success(data, request.getRequestURI()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> create(
            @RequestBody Map<String, Object> payload,
            HttpServletRequest request
    ) {
        Map<String, Object> data = Map.of(
                "id", 1L,
                "title", payload.getOrDefault("title", "신규")
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(data, request.getRequestURI()));
    }

    @GetMapping("/error")
    public void error() {
        throw new CustomException(ErrorCode.INVALID_INPUT);
    }
}
