# 📌 03 — spring-security-jwt
## 🚀 개요
Spring Security의 내부 인증 흐름과 JWT 기반 토큰 인증 구조를 분석하고, 필터 체인·토큰 발급·인가·로그아웃·토큰 재발급 등 실무 인증 서버 기능을 구현하는 프로젝트입니다.

## 🧱 인증/인가 아키텍처 흐름
### 🔹 전체 요청 흐름
```
Request
 → Security Filter Chain
      → JwtAuthenticationFilter
           → Token 추출
           → 검증
           → Authentication 생성
           → SecurityContextHolder 저장
 → Controller
```
### 🔹 로그인 흐름
```
ID/PW 입력
→ AuthenticationManager
     → UserDetailsService
         → UserDetails 반환
→ PasswordEncoder(BCrypt) matches()
→ 성공 시 JWT Access/Refresh Token 발급
```
### 🔹 토큰 재발급
```
Access Token 만료
 → Refresh Token 검증
      → Access Token 재발급
```
### 🔹 로그아웃
```
로그아웃 요청
 → Refresh Token 삭제
 → Access Token blacklist 등록(필요 시 Redis)
```
## 🔍 실습 주제 목록
### ✔ Security 필터 체인 분석
- OncePerRequestFilter
- UsernamePasswordAuthenticationFilter 대체

### ✔ JWT 설계
- Access / Refresh Token 전략
- Type 검증 로직 구현
- 만료 시간 / 보안 옵션 설계

### ✔ 인가 처리
- ROLE 기반 매핑
- @PreAuthorize, @Secured

### ✔ 로그인/회원가입
- PasswordEncoder
- UserDetails / UserDetailsService

### ✔ Refresh Token 저장 전략
- DB(PostgreSQL)
- Redis
- HttpOnly Cookie

## 📦 JWT Payload 예시
```json
{
  "sub": "userId",
  "roles": ["USER"],
  "type": "ACCESS",
  "iat": 1710000000,
  "exp": 1710003600
}
```

## 📦 공통 Response, Error 템플릿
- API Success Response Specification.md 참고
- Error Response Specification.md 참고

## 🔐 비밀번호 저장 정책(BCrypt)
- 회원 비밀번호는 BCrypt로 해시되어 저장됩니다. 평문 비밀번호는 DB에 절대 저장하지 않습니다.
- 인증 시에도 입력값은 해시 비교(`PasswordEncoder.matches`)로 검증합니다.
- 구성
  - PasswordEncoder: `BCryptPasswordEncoder(workFactor)`
  - Work factor(라운드)는 설정으로 조절 가능합니다.
    - `application.properties`
      ```properties
      # BCrypt work factor (높을수록 보안 ↑, 성능 ↓). 권장 10~14
      app.security.password.bcrypt-strength=12
      ```
  - 기본값은 10이며, 운영 환경에서는 12 이상을 권장합니다. 서버 성능과 트래픽을 고려해 조정하세요.
- 마이그레이션 팁
  - 기존 평문/다른 해시 사용 프로젝트에서 넘어오는 경우, 최초 로그인/비밀번호 변경 시점에 재해시(BCrypt)하도록 처리하는 전략을 권장합니다.

## 👤 현재 사용자 접근 방법(예제 모음)
컨트롤러/서비스에서 현재 인증 정보를 꺼내는 다양한 방법을 제공합니다. 상황에 따라 가장 간단한 방법을 선택하세요.

### 1) SecurityUtil(프로젝트 공통 유틸)
```java
String username = SecurityUtil.getUsername();
Long userId = SecurityUtil.requireUserId();
List<String> roles = SecurityUtil.getRoles();
boolean isAdmin = SecurityUtil.hasRole("ADMIN");
```

### 2) 메서드 파라미터 주입 방식
- Authentication 주입
```java
@PreAuthorize("isAuthenticated()")
@GetMapping("/api/samples/auth/authentication")
public ApiResponse<?> sample(Authentication authentication) {
    String username = authentication.getName();
    Long userId = (authentication.getPrincipal() instanceof CustomUserDetails cud) ? cud.getId() : null;
    return ApiResponse.success(Map.of("username", username, "userId", userId), "/api/samples/auth/authentication");
}
```

- java.security.Principal 주입(사용자명만 필요할 때)
```java
@GetMapping("/api/samples/auth/principal")
public ApiResponse<?> sample(Principal principal) {
    return ApiResponse.success(Map.of("username", principal.getName()), "/api/samples/auth/principal");
}
```

- @AuthenticationPrincipal로 커스텀 Principal 직접 주입
```java
@GetMapping("/api/samples/auth/authentication-principal")
public ApiResponse<?> sample(@AuthenticationPrincipal CustomUserDetails principal) {
    return ApiResponse.success(Map.of("id", principal.getId(), "username", principal.getUsername()), "/api/samples/auth/authentication-principal");
}
```

- @AuthenticationPrincipal + SpEL로 특정 필드만 주입
```java
@GetMapping("/api/samples/auth/authentication-principal/id")
public ApiResponse<Long> sample(@AuthenticationPrincipal(expression = "id") Long userId) {
    return ApiResponse.success(userId, "/api/samples/auth/authentication-principal/id");
}
```

- @CurrentSecurityContext로 SecurityContext/Authentication 직접 주입
```java
@GetMapping("/api/samples/auth/current-context")
public ApiResponse<?> sample(@CurrentSecurityContext SecurityContext context) {
    Authentication auth = context.getAuthentication();
    return ApiResponse.success(Map.of("name", auth.getName()), "/api/samples/auth/current-context");
}
```

위 예제들은 `AuthSamplesController` 에 구현되어 있으며, `@PreAuthorize` 로 인가를 적용한 샘플입니다.
