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
