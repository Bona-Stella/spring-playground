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

/**
 * 전역 예외 처리기 (Global Exception Handler)
 *
 * @RestControllerAdvice: 모든 컨트롤러(@RestController)에서 발생하는 예외를 가로채서 처리합니다.
 * 각 마이크로서비스(Order, Chat 등)는 이 클래스를 스캔하여 동일한 포맷의 에러 응답(JSON)을 반환하게 됩니다.
 */
@RestControllerAdvice
public class CommonExceptionHandler {

    /**
     * [MSA 추적 로직] 에러 메시지에 Trace ID 추가
     *
     * Gateway에서 생성하여 헤더에 넣어준 'X-Trace-Id'를 꺼내 에러 메시지 뒤에 붙입니다.
     * 이유: 수많은 로그 중에서 "어떤 사용자의 요청에서 에러가 났는지" 빠르게 찾기 위함입니다.
     * 예: "잘못된 입력입니다 [traceId=a1b2c3d4]"
     */
    private String withTraceId(HttpServletRequest request, String baseMessage) {
        String traceId = request.getHeader("X-Trace-Id");
        // Trace ID가 없으면 기본 메시지만 반환, 있으면 붙여서 반환
        if (traceId == null || traceId.isBlank()) return baseMessage;
        return baseMessage + " [traceId=" + traceId + "]";
    }

    /**
     * 1. 유효성 검사 실패 예외 (400 Bad Request)
     *
     * @Valid 어노테이션을 사용한 DTO 검증 실패 시 발생합니다 (MethodArgumentNotValidException).
     * 예: "이메일 형식이 아닙니다", "비밀번호는 8자 이상이어야 합니다" 등
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        // 여러 필드 에러 중 '첫 번째' 에러 메시지만 추출하여 사용자에게 전달 (간결함 유지)
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(e -> e.getField() + " " + e.getDefaultMessage()) // 예: "email 이메일 형식이 올바르지 않습니다."
                .orElse(ErrorCode.VALIDATION_ERROR.message());

        // Trace ID 추가
        message = withTraceId(request, message);

        // 표준 에러 응답 객체(ErrorResponse) 생성
        ErrorResponse body = new ErrorResponse(
                false,                                      // success 여부
                ErrorCode.VALIDATION_ERROR.status(),        // HTTP 상태 코드 (400)
                ErrorCode.VALIDATION_ERROR.name(),          // 에러 코드명
                message,                                    // 상세 메시지
                java.time.ZonedDateTime.now().toString(),   // 발생 시간
                request.getRequestURI()                     // 요청 경로
        );
        return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.status()).body(body);
    }

    /**
     * 2. 잘못된 요청 예외 그룹 (400 Bad Request)
     *
     * 클라이언트가 요청을 잘못 보낸 경우 발생하는 다양한 예외들을 묶어서 처리합니다.
     * - ConstraintViolationException: JPA 유효성 검사 실패
     * - IllegalArgumentException: 부적절한 인자 전달 (비즈니스 로직)
     * - IllegalStateException: 부적절한 상태에서의 요청
     * - HttpMessageNotReadableException: JSON 파싱 실패 (예: 숫자에 문자를 넣음)
     */
    @ExceptionHandler({ConstraintViolationException.class, IllegalArgumentException.class, IllegalStateException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception ex, HttpServletRequest request) {
        // "잘못된 입력입니다" 같은 공통 메시지에 Trace ID를 붙임
        String msg = withTraceId(request, ErrorCode.INVALID_INPUT.message());

        ErrorResponse body = new ErrorResponse(
                false,
                ErrorCode.INVALID_INPUT.status(),
                ErrorCode.INVALID_INPUT.name(),
                msg,
                java.time.ZonedDateTime.now().toString(),
                request.getRequestURI()
        );
        return ResponseEntity.status(ErrorCode.INVALID_INPUT.status()).body(body);
    }

    /**
     * 3. 알 수 없는 서버 내부 에러 (500 Internal Server Error)
     *
     * 위에서 잡지 못한 모든 예외(NullPointerException, DB 연결 오류 등)를 처리하는 최후의 보루입니다.
     * 보안상 구체적인 에러 내용(Stack Trace)은 클라이언트에게 숨기고, "서버 내부 오류"라는 메시지만 보냅니다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAny(Exception ex, HttpServletRequest request) {
        String msg = withTraceId(request, ErrorCode.INTERNAL_SERVER_ERROR.message());

        ErrorResponse body = new ErrorResponse(
                false,
                ErrorCode.INTERNAL_SERVER_ERROR.status(),
                ErrorCode.INTERNAL_SERVER_ERROR.name(),
                msg,
                java.time.ZonedDateTime.now().toString(),
                request.getRequestURI()
        );
        return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.status()).body(body);
    }
}
