#📌 04 — spring-redis-jobs
## 🚀 개요
Redis를 이용한 캐싱/세션/분산 락/실시간 메시징과
Spring Scheduler 기반의 백그라운드 작업 등을 결합해
실서비스 운영 레벨 기능을 실험하는 프로젝트입니다.

## 🧱 Redis 활용 구조
###🔹 캐싱 Flow
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
