# 📌 02 — spring-aop-lab

## 🚀 개요
Spring AOP의 내부 동작 원리인 프록시 기반 메커니즘과 애스펙트를 활용하여 로깅, 트랜잭션 경계, 측정 기능 등을 실험하는 프로젝트입니다.

## 🧱 AOP 아키텍처 흐름
### 🔹 프록시 기반 호출 흐름
```
Client
 → Proxy (JDK Dynamic Proxy / CGLIB)
     → Advice 적용
         → JoinPoint (Target Method)
             → 실제 비즈니스 로직 실행
     → After / AfterReturning / AfterThrowing
```
### 🔹 트랜잭션 경계
```
Method Call
  → @Transactional Advice
       → TransactionManager 시작
       → 실제 메서드 실행
       → 정상 → commit
       → 예외 → rollback
```
## 🔍 실습 주제 목록
### ✔ 로깅 AOP
- 메서드 호출/종료 로그 자동 출력
- 실행 시간 측정
### ✔ 커스텀 애노테이션 실험
- @LogExecutionTime
- @Masking 등 도메인 레벨 기능 실험
### ✔ 예외 변환 AOP
- 특정 도메인 오류 자동 변환
### ✔ 트랜잭션 AOP 이해
- 프록시 동작 확인
- 내부 호출(self-invocation) 문제 재현

## 📦 AOP용 예시 템플릿
```java
@Around("@annotation(LogExecutionTime)")
public Object measure(ProceedingJoinPoint joinPoint) throws Throwable {
    long start = System.currentTimeMillis();
    Object result = joinPoint.proceed();
    long end = System.currentTimeMillis();
    log.info("[{}] executed in {}ms", joinPoint.getSignature(), end - start);
    return result;
###}
```

