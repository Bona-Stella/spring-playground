# 📌 01 — spring-api-board

## 🚀 개요
기본적인 Spring MVC 기반 API 서버의 표준 구조를 정립하고, 실제 서비스에서 흔히 쓰이는 CRUD / 검색 / 파일 업로드 / 문서화를 실습하는 프로젝트입니다.
+ 기본 API 서버
+ 게시판
+ Querydsl
+ Swagger
+ 파일 업로드

## 🧱 아키텍처 흐름
### 요청 처리 플로우 (MVC 구조)
```
Client
  → Controller
      → Validation
      → Service
           → Repository (JPA / Querydsl)
           → Domain Logic
      → DTO 변환
  → Response Wrapper(ApiResponse)
```
### 예외 처리 흐름
```
Controller / Service
   → 예외 발생
   → @RestControllerAdvice
        → Custom Exception 변환
        → ApiErrorResponse 생성
        → 반환
```
### 업로드 흐름
```
Multipart Request
  → FileController
       → FileService
            → Local/S3 저장
            → 파일 메타데이터 DB 저장(optional)
  → 다운로드 URL 반환
```
### 문서화(Swagger/OpenAPI)
```
SwaggerConfig
  → SpringDoc OpenAPI 자동 스캔
      → Controller / DTO 문서 생성
      → Swagger UI 제공
```
## 🔍 실습 주제 목록
### 📌 API 서버 기본기
- Controller/Service/Repository 구조화
- DTO / Entity 분리 전략
- Layered Architecture 적용
### 📌 유효성 검증
- @Valid + Bean Validation
- 커스텀 Validator
### 📌 예외 처리
- 글로벌 예외 처리기
- ErrorCode Enum 설계
### 📌 Querydsl
- 조건부 동적 검색
- 페이징 처리
- 정렬 및 복합 검색
### 📌 파일 업로드/다운로드
- MultipartFile 처리
- Local File / AWS S3 저장 구조 확장 가능
### 📌 Swagger 문서화
- OpenAPI 문서 생성
- Grouping
- Example 값 설정

## 📦 공통 Response, Error 템플릿
- API Success Response Specification.md 참고
- Error Response Specification.md 참고

