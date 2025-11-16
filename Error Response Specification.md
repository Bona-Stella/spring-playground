## ✅ 📘 통일된 Error Response 표준
## 🔥 최종 Error Response JSON (전 서비스 공통)
```json
{
  "success": false,
  "status": 400,
  "code": "INVALID_INPUT",
  "message": "Request validation failed.",
  "timestamp": "2025-01-01T12:30:02Z",
  "path": "/api/v1/users"
}
```
## 필드 설명
| 필드          | 설명                                           |
| ----------- | -------------------------------------------- |
| `success`   | 성공/실패 여부 명확 표시                               |
| `status`    | HTTP Status Code                             |
| `code`      | 도메인별/카테고리별 에러 코드 (문자열 Enum)                  |
| `message`   | 사용자/개발자 모두 읽기 쉬운 에러 설명                       |
| `timestamp` | ISO-8601                                     |
| `path`      | 요청 경로 (Filter or HandlerInterceptor에서 자동 주입) |

## 🎯 에러 코드 설계 (전 프로젝트 공통 Enum)
```java
public enum ErrorCode {

    // 400 BAD REQUEST
    INVALID_INPUT(400, "잘못된 요청입니다."),
    VALIDATION_ERROR(400, "입력값이 올바르지 않습니다."),

    // 401 UNAUTHORIZED
    UNAUTHORIZED(401, "인증이 필요합니다."),
    INVALID_TOKEN(401, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(401, "만료된 토큰입니다."),

    // 403 FORBIDDEN
    ACCESS_DENIED(403, "접근 권한이 없습니다."),

    // 404 NOT FOUND
    NOT_FOUND(404, "요청한 리소스를 찾을 수 없습니다."),
    USER_NOT_FOUND(404, "해당 사용자가 존재하지 않습니다."),

    // 409 CONFLICT
    DUPLICATE_RESOURCE(409, "이미 존재하는 리소스입니다."),

    // 500 INTERNAL SERVER ERROR
    INTERNAL_SERVER_ERROR(500, "서버 오류가 발생했습니다.");

    private final int status;
    private final String message;

    ErrorCode(int status, String message) {
        this.status = status;
        this.message = message;
    }

    public int status() { return status; }
    public String message() { return message; }
}
```

## 📦 Error Response DTO (전 프로젝트 공통)
```java
public record ErrorResponse(
        boolean success,
        int status,
        String code,
        String message,
        String timestamp,
        String path
) {
    public static ErrorResponse of(ErrorCode code, String path) {
        return new ErrorResponse(
                false,
                code.status(),
                code.name(),
                code.message(),
                java.time.ZonedDateTime.now().toString(),
                path
        );
    }
}
```
## 🛠 공통 글로벌 예외 처리기 (전 프로젝트 공통)
```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(
            CustomException e, HttpServletRequest request) {
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.status())
                .body(ErrorResponse.of(errorCode, request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException e, HttpServletRequest request) {
        ErrorCode errorCode = ErrorCode.VALIDATION_ERROR;
        return ResponseEntity
                .status(errorCode.status())
                .body(ErrorResponse.of(errorCode, request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception e, HttpServletRequest request) {
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity
                .status(errorCode.status())
                .body(ErrorResponse.of(errorCode, request.getRequestURI()));
    }
}
```

## 📘 CustomException 만들기 (전 프로젝트 공통)
```java
public class CustomException extends RuntimeException {
    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.message());
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
```
