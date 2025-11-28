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

### ✔ 세션 기반 로그인(추가)
- Spring Session(Redis)로 서버 세션 저장
- JWT 흐름과 병행 운영: 경로 분리(`/api/session/**`)

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

## 🧪 세션 기반 로그인 추가 안내
본 프로젝트는 JWT 기반 인증 외에, 동일한 인증 모델을 세션 기반으로도 사용할 수 있도록 병행 구성을 제공합니다. 라우팅으로 흐름을 분리하여 서로 간섭 없이 동작합니다.

### 활성화 개요
- 의존성: `spring-session-data-redis`
- 설정: `spring.session.store-type=redis`, `server.servlet.session.timeout=30m` 등
- 보안 체인 분리: `SecurityFilterChain` 2개
  - 체인 #0 (세션): `securityMatcher("/api/session/**", "/h2-console/**", "/actuator/health")`, `SessionCreationPolicy.IF_REQUIRED`
  - 체인 #1 (JWT): 나머지 요청, `SessionCreationPolicy.STATELESS`
- 공통: 필터 단계 예외는 `ExceptionHandlingFilter` 통해 전역 예외 처리기로 위임

### 세션 API 엔드포인트
- `POST /api/session/login` — 세션 로그인(permitAll)
  - 요청: `{ "username": "id", "password": "pw" }`
  - 성공 시: `JSESSIONID` 쿠키가 발급되며, 응답 바디는 사용자 요약 정보(`userId`, `username`, `roles`)를 담은 `ApiResponse` 포맷
- `GET /api/session/me` — 현재 세션 사용자 정보 조회(인증 필요)
- `POST /api/session/logout` — 세션 로그아웃(인증 필요)

모든 응답은 기존과 동일한 `ApiResponse<T>` 포맷입니다.

### 호출 예시(curl)
1) 로그인(세션 생성)
```
curl -i -c cookie.txt -H "Content-Type: application/json" \
     -d '{"username":"alice","password":"pass"}' \
     http://localhost:8080/api/session/login
```
2) 인증 요청(세션 유지)
```
curl -b cookie.txt http://localhost:8080/api/session/me
```
3) 로그아웃
```
curl -X POST -b cookie.txt http://localhost:8080/api/session/logout
```

### CSRF 관련
- 본 예제의 세션 체인은 API 학습 편의를 위해 CSRF를 비활성화했습니다.
- 브라우저 기반 폼/페이지에서 운영 시에는 `CookieCsrfTokenRepository` 등으로 CSRF를 활성화하는 것을 권장합니다.

### JWT 흐름과의 관계
- 기존 JWT 엔드포인트(`/api/auth/**`, 보호 API)는 그대로 유지되며, 세션 엔드포인트는 `/api/session/**`로 분리되어 서로 영향을 주지 않습니다.
- 두 방식은 동시에 서비스 가능하며, 클라이언트는 요구사항에 따라 적절한 방식을 선택하면 됩니다.
