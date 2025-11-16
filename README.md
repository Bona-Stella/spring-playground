# spring-playground
Spring Framework &amp; Spring Boot 기능을 실험하는 플레이그라운드

📘 Spring Playground

Spring Framework & Spring Boot 전반의 구조적 특징과 주요 기술 스택을 실전 중심으로 탐구하는 레포입니다.
핵심 기능의 내부 동작 원리를 분석하고, 다양한 아키텍처 패턴을 직접 적용·실험합니다.

🎯 목표 (Goals)

Spring Core의 내부 동작 원리(빈 라이프사이클, 프록시 기반 AOP, 클래스패스 스캔)를 구조적으로 분석한다.

Spring MVC와 WebFlux의 요청 처리 파이프라인을 비교하며, 적절한 선택 기준을 정리한다.

데이터 접근 기술(JPA, Querydsl, Spring Data)의 장단점과 내부 추상화를 이해하고, 복합 쿼리 설계 패턴을 실습한다.

테스트 전략(Unit, Slice, Integration)을 상황별로 설계하고, 테스트 비용 최적화 방식을 탐구한다.

실무에서 자주 쓰이는 구조(Service Layer, Port & Adapter, Domain Layer)를 스프링 기반으로 아키텍처 실험한다.

Spring Boot 자동 설정 원리를 살펴보고, 필요 시 커스텀 AutoConfiguration을 구성해본다.

다양한 기술 스택(REST API, Validation, Security, Message Converter, Scheduler)을 통합하여 일관된 애플리케이션 구조를 정립한다.

📘 Spring Playground

Spring Boot 기반 백엔드 개발에 사용되는 주요 기술 스택을 전반적으로 실험하고, 실무적 적용 관점에서 구조와 동작 원리를 분석하는 레포입니다.
스프링의 핵심 메커니즘을 이해하고, 다양한 인프라 및 아키텍처 패턴을 테스트하는 개인 연구용 프로젝트입니다.

🎯 목표 (Goals)

Spring Core 내부 구조(Bean Lifecycle, Proxy 기반 AOP, Component Scanning)를 깊이 있게 이해하고 분석한다.

Spring MVC의 요청 처리 과정(DispatcherServlet → Handler Mapping → Interceptor → Argument Resolver)을 구조적으로 파악한다.

Spring Security 기반 인증/인가 흐름, JWT Token 기반 인증 전략을 실전 수준으로 구성한다.

Redis를 활용한 캐싱, 세션 관리, 분산 락 등 고급 활용 패턴을 실험한다.

JPA, Querydsl, Spring Data를 비교하며 도메인 중심 설계 및 데이터 접근 전략을 정립한다.

Swagger / SpringDoc OpenAPI를 사용해 API 문서화를 자동화하고, 실무와 유사한 구조를 만든다.

Validation, Exception Handling, Logging, Scheduler 등 백엔드 서비스 운영에 필요한 범용 기능들을 정리한다.

다수의 Java/Spring 기반 미니 프로젝트를 통해 아키텍처·테스트·모듈화 패턴을 검증한다.



🛠 기술 스택 (Tech Stack)
언어 & 런타임
- Java 21+
- Gradle-Kotlin

Spring Framework / Boot
- Spring Boot (Auto Configuration 분석 포함)
- Spring MVC
- Spring AOP
- Spring Security
- Spring Validation
- Spring Cache (with Redis)

인증 & 보안
- Spring Security
- JWT (Access/Refresh Token 전략)
- Password Encoding
- CORS/CSRF 전략 실험

데이터 접근
- JPA / Hibernate
- Querydsl
- Spring Data JPA
- JDBC Template (필요 시)

데이터베이스 & 인프라
- PostgreSQL (RDBMS)
- Redis (캐싱, 세션, 분산 락, 메시지 큐 실험 가능)
- Testcontainers (DB/Redis 통합 테스트)

문서화 & API 도구
- Swagger / SpringDoc OpenAPI
- RESTful API 스타일 가이드 정립
- Error Response 표준화

테스트
- JUnit5
- Mockito
- Spring Boot Test
- Integration Test (Containers)

기타 실무용 라이브러리 / 기능
- Lombok
- MapStruct (DTO ↔ Entity 변환)
- Spring Scheduler
- Logging (SLF4J, Logback), MDC 적용
- ExceptionHandler 기반 글로벌 예외 처리

📝 학습 로그 (Learning Notes)
각 기능/프로젝트별 README 또는 /docs 폴더에 구조적 정리를 포함해 기록할 예정입니다.

