# 📌 04 — spring-redis-jobs
## 🚀 개요
Redis를 이용한 캐싱/세션/분산 락/실시간 메시징과
Spring Scheduler 기반의 백그라운드 작업 등을 결합해
실서비스 운영 레벨 기능을 실험하는 프로젝트입니다.

## 🧱 Redis 활용 구조
### 🔹 캐싱 Flow
```
Controller
  → Service
      → @Cacheable
          → Redis Hit? → 데이터 반환
          → Miss → DB 조회 후 Redis 저장
```
### 🔹 분산 락
```
Scheduler or API
 → Redis SET NX PX(lockKey)
     → Lock 획득 시 critical task 실행
     → 실패 시 → 병행 작업 차단
```
### 🔹 세션 저장소
```
Spring Session
 → SessionRepositoryFilter
      → Redis Session Store
```
### 🔹 Pub/Sub
```
Publisher → Redis Channel → Subscriber (Listener Container)
```

## 🔄 Scheduler 흐름
```
Scheduler
  → 잡 실행
       → Redis 락 획득
           → 작업 수행 (집계/캐시 갱신 등)
           → 락 해제
```

## 동기 / 비동기 워크플로우 (내부 / 외부 · MQ 포함)
### 🔹 내부 동기 Flow
```
Controller → Service
    → (옵션) Redis 캐시 조회
    → DB 조회 / 도메인 로직 수행
    → (옵션) Redis 캐시 저장
  → 응답 반환
```
### 🔹 내부 비동기 Flow (MQ)
```
Controller → Service
    → 핵심 로직만 처리
    → MQ Producer 로 비동기 이벤트 발행
  → 응답 반환
```
### 🔹 MQ
```
→ Consumer
  → (옵션) Redis 분산 락 획득
    → 장기 실행 작업 / 후처리 수행
    → DB / Redis 업데이트
    → 락 해제
```
### 🔹 외부 동기 Flow
```
Service
  → 외부 API 동기 호출
    → 재시도/서킷브레이커 처리
  → 응답 데이터 가공
  → (옵션) Redis 캐시 저장
```
### 🔹 외부 비동기 Flow (Webhook·MQ는 제외)
```
외부 시스템 이벤트
  → MQ Producer
MQ
  → Consumer
    → (옵션) Redis 분산 락
    → 내부 상태( DB / Redis ) 반영
    → 락 해제
```
## 🔍 실습 주제 목록
### ✔ 캐싱 패턴
- @Cacheable, @CacheEvict
- 캐시 무효화 전략
### ✔ Redis 세션
- 로그인 세션 유지
- 서버 확장 대비 세션 공유
### ✔ Redis 분산 락
- 재고 감소 안정성 테스트
- API 중복 호출 방지
### ✔ Pub/Sub
- 간단한 알림 시스템
### ✔ Scheduler + Redis
- 캐시 리빌드
- 만료 데이터 정리
- 주기적 배치
### ✔ 내부/외부 동기·비동기 + MQ
- 내부 동기 처리 흐름 (캐시 → DB → 캐시 업데이트)
- 내부 비동기 처리 (MQ 발행 → Consumer 후처리)
- 외부 API 동기 호출 및 캐싱
- 외부 이벤트 비동기 처리 (MQ 기반 후속 로직)

## 📦 Redis 연동 예시 (Lock)
```java
Boolean locked = redisTemplate
        .opsForValue()
        .setIfAbsent("lock:job", "1", Duration.ofSeconds(10));

if (Boolean.TRUE.equals(locked)) {
    try {
        runJob();
    } finally {
        redisTemplate.delete("lock:job");
    }
}
```
## 📦 공통 Response, Error 템플릿
- API Success Response Specification.md 참고
- Error Response Specification.md 참고



