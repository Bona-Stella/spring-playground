## ✅ 📘 통일된 API Response (Success Response)
(모든 프로젝트 공통 표준)

##🔥 최종 Success Response JSON 형태
```json
{
  "success": true,
  "status": 200,
  "code": "OK",
  "message": "Success",
  "data": {
    "id": 1,
    "title": "게시글 제목"
  },
  "timestamp": "2025-01-01T12:30:02Z",
  "path": "/api/v1/posts/1"
}
```
## 🧩 필드 설명
| 필드          | 설명                          |
| ----------- | --------------------------- |
| `success`   | 성공 여부(true) 명시              |
| `status`    | HTTP 상태 코드                  |
| `code`      | 성공 코드 (기본: `OK`, `CREATED`) |
| `message`   | 직관적인 성공 메시지                 |
| `data`      | 실제 비즈니스 응답 데이터              |
| `timestamp` | ISO-8601                    |
| `path`      | 요청된 API Path                |

## 🧱 ApiResponse DTO (전 프로젝트 공통)
```java
public record ApiResponse<T>(
        boolean success,
        int status,
        String code,
        String message,
        T data,
        String timestamp,
        String path
) {

    public static <T> ApiResponse<T> success(T data, String path) {
        return new ApiResponse<>(
                true,
                200,
                "OK",
                "Success",
                data,
                java.time.ZonedDateTime.now().toString(),
                path
        );
    }

    public static <T> ApiResponse<T> created(T data, String path) {
        return new ApiResponse<>(
                true,
                201,
                "CREATED",
                "Resource created successfully.",
                data,
                java.time.ZonedDateTime.now().toString(),
                path
        );
    }

    public static <T> ApiResponse<T> of(int status, String code, String message, T data, String path) {
        return new ApiResponse<>(
                true,
                status,
                code,
                message,
                data,
                java.time.ZonedDateTime.now().toString(),
                path
        );
    }
}
```

## 🔗 Controller에서 사용하는 예시
```java
✔ 조회 성공
@GetMapping("/{id}")
public ResponseEntity<ApiResponse<PostDto>> getPost(
        @PathVariable Long id,
        HttpServletRequest request
) {
    PostDto post = postService.get(id);
    return ResponseEntity.ok(ApiResponse.success(post, request.getRequestURI()));
}

✔ 생성 성공
@PostMapping
public ResponseEntity<ApiResponse<PostDto>> create(
        @Valid @RequestBody CreatePostRequest requestDto,
        HttpServletRequest request
) {
    PostDto created = postService.create(requestDto);
    return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.created(created, request.getRequestURI()));
}
```
## 🧩 HTTP Status → ApiResponse 매핑 컨벤션
| 상황      | 응답 방법                         |
| ------- | ----------------------------- |
| 조회 성공   | `ApiResponse.success()` + 200 |
| 생성 성공   | `ApiResponse.created()` + 201 |
| 업데이트 성공 | `success()` + 200             |
| 삭제 성공   | 메시지만 응답하거나 data 없이 success()  |
| 페이징     | data 안에 page 정보 통합            |
