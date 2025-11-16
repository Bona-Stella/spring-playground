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
 → PasswordEncoder matches()
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

📦 JWT Payload 예시
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

