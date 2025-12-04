# 📌 05 — spring-msa-mq

## 🚀 프로젝트 개요
이 프로젝트는 단순한 비즈니스 로직 구현이 아닌, 대규모 트래픽 환경에서 백엔드 시스템이 겪는 문제들을 기술적으로 해결하는 데 초점을 둡니다.
### 핵심 실험 주제:
1. Redis의 4가지 얼굴: 캐싱(Caching), 분산 락(Lock), 세션 저장소(Session), 실시간 메시징(Pub/Sub).
2. 비동기 통신의 정석: @Async vs RabbitMQ vs Kafka의 명확한 사용 기준 정립.
3. MSA 트랜잭션: 분산 환경에서 데이터 일관성을 맞추는 방법 (Eventual Consistency).
4. Java 21: Virtual Threads를 활용한 고성능 Consumer 구현.
## 🧱 전체 아키텍처 및 기술 구성
### 1. 서비스 구성 (MSA)
| 서비스 명               | 포트   | 역할 및 주요 기술                                                     |
| ------------------- | ---- | -------------------------------------------------------------- |
| **Gateway Service** | 8080 | 라우팅, 로드밸런싱, SSL 종단                                             |
| **Auth Service**    | 8081 | 로그인/회원가입, **Redis Session Clustering**                         |
| **Order Service**   | 8082 | 주문/결제, **Redis Caching**, **Distributed Lock**, RabbitMQ(Prod) |
| **Chat Service**    | 8083 | 라이브 채팅, **Redis Pub/Sub**, Kafka(Prod)                         |
| **Worker Service**  | 8084 | 비동기 후처리(알림, 로그적재), **Java 21 Virtual Threads**, MQ Consumer    |
### 2. 미들웨어 활용 구조
#### 🔹 Redis (In-Memory Performance)
- Caching: DB 조회 부하 감소 (Look-aside 패턴).
- Session Store: MSA 간 로그인 세션 공유.
- Distributed Lock: 재고 차감 등 동시성 제어.
- Pub/Sub: 실시간 웹소켓 메시지 브로드캐스팅.  
#### 🔹 RabbitMQ (Task Reliability)
- 용도: 반드시 처리되어야 하는 작업 (주문 접수, 이메일 발송).
- 특징: 메시지 확인(Ack), 재시도(Retry), 실패 격리(DLQ) 보장.  
#### 🔹 Apache Kafka (Data Streaming)
- 용도: 대용량 데이터 수집 및 기록 (채팅 로그, 클릭 스트림, 통계).
- 특징: 압도적인 처리량, 디스크 기반 영구 저장, 배치 처리 용이.
## 🔄 상세 워크플로우 (Flow)
### 1. 동기 처리 Flow (캐싱 & 세션)
#### 상황: 상품 상세 조회, 로그인 검증
```
[User]
  ↓ (HTTP Request)
[API Gateway] 
  → Redis Session Store 조회 (로그인 여부 확인)
  ↓
[Order Service]
  → 1. Redis Cache 조회 (@Cacheable)
      → Hit? 데이터 반환 (0.5ms)
      → Miss? DB 조회 → Redis 저장 → 반환 (100ms)
```
### 2. 고동시성 제어 Flow (분산 락 & MQ)
#### 상황: 선착순 100개 한정 판매 (재고 감소)
```
[User]
  ↓ (주문 요청)
[Order Service]
  → 1. Redis Distributed Lock 획득 (Key: "item:100:lock")
  → 2. 재고 확인 및 감소 (Redis or DB)
  → 3. RabbitMQ에 "주문 생성 이벤트" 발행 (Async)
  → 4. Lock 해제
  → 5. 사용자에게 "접수됨" 응답 (Non-Blocking)

[Worker Service (Consumer)]
  → RabbitMQ에서 메시지 수신 (Virtual Thread)
  → DB에 실제 주문 데이터 Insert (트랜잭션)
  → 실패 시 Retry / DLQ 이동
```
### 3. 실시간 & 아카이빙 Flow (Pub/Sub & Kafka)
#### 상황: 라이브 방송 채팅
```
[User A]
  ↓ (WS Message)
[Chat Service]
  → 1. Redis Pub/Sub 발행 (즉시성, 휘발성)
       ↘ (구독 중인 모든 채팅 서버가 받아서 User B, C에게 전송)
  → 2. Kafka Topic 발행 (저장성, 영속성)
       ↘ (Worker Service가 천천히 가져가서 MongoDB에 저장)
```
### 4. 스케줄러 & 배치 Flow
#### 상황: 통계 집계, 만료 데이터 정리
```
[Spring Scheduler]
  → 1. Redis Lock 획득 ("batch:daily-stat")
       (스케줄러가 여러 서버에서 돌아도 중복 실행 방지)
  → 2. Kafka/DB에서 데이터 읽어서 통계 생성
  → 3. Redis Cache 갱신 (@CacheEvict or @CachePut)
  → 4. Lock 해제
```
## 🔍 실습 상세 시나리오
### ✔ Topic 1: Redis Caching & Session
- 목표: DB 부하를 줄이고 서버 간 세션을 공유한다.
- 실습:
  - @Cacheable을 적용하여 동일한 API 호출 시 DB 쿼리가 안 나가는지 확인.
  - Gateway에서 로그인하고 Order Service에서 세션 정보를 읽어올 수 있는지 확인 (Redis Session).
### ✔ Topic 2: Redis 분산 락 (Distributed Lock)
- 목표: 동시성 이슈(Race Condition) 해결.
- 실습:
  - JMeter로 재고 100개 상품에 1000명 동시 요청.
  - synchronized (실패) vs Lettuce Lock (스핀락 부하) vs Redisson (성공) 성능 비교.
### ✔ Topic 3: RabbitMQ를 이용한 시스템 분리
- 목표: 주문 서비스가 결제/알림 서비스의 장애에 영향을 받지 않게 한다.
- 실습:
  - RabbitMQ를 끄거나 Worker Service를 강제 종료한 상태에서 주문 요청.
  - 주문은 정상 접수되고, 서버 복구 시 밀린 작업이 처리되는지 확인.
### ✔ Topic 4: Kafka 채팅 아카이빙
- 목표: Redis의 휘발성 데이터를 Kafka로 영구 저장한다.
- 실습:
  - 채팅방에서 10만 건의 메시지 폭탄 전송.
  - 실시간 대화는 Redis로 렉 없이 진행됨을 확인.
  - 약간의 딜레이(Lag)가 있더라도 MongoDB에 10만 건이 하나도 빠짐없이 저장되는지 확인.
##📦 핵심 코드 예시
### 1. Redis 분산 락 (Redisson)
```java
@Transactional
public void decreaseStock(Long itemId, int quantity) {
    RLock lock = redissonClient.getLock("stock:" + itemId);
    
    try {
        // 10초간 락 획득 시도, 획득 후 3초 뒤 자동 해제
        if (lock.tryLock(10, 3, TimeUnit.SECONDS)) {
            // 핵심 비즈니스 로직
            Stock stock = stockRepository.findById(itemId);
            stock.decrease(quantity);
        }
    } catch (InterruptedException e) {
        throw new RuntimeException("Lock acquisition failed");
    } finally {
        if (lock.isLocked() && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
```
### 2. RabbitMQ Configuration (With Virtual Threads)
```java
@Bean
public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    factory.setConnectionFactory(connectionFactory);
    // Java 21 가상 스레드 적용으로 처리량 극대화
    factory.setTaskExecutor(new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor()));
    return factory;
}
```
3. Kafka Producer (Archiving)
```java
public void sendChatLog(ChatMessage message) {
    // 키를 roomId로 설정하여 동일 방의 메시지 순서 보장
    kafkaTemplate.send("chat-log-topic", message.getRoomId(), message);
}
```
## 📦 공통 Response, Error 템플릿
- API Success Response Specification.md 참고
- Error Response Specification.md 참고





## Phase 5 운영 가이드 — 사용법(옵션 B 블랙리스트 + 사가 보상)

아래는 추가 구현된 두 기능의 실행/검증 방법입니다.

### 1) 옵션 B: 푸시형 인메모리 블랙리스트(게이트웨이에서 즉시 차단)
- 개요: Auth가 로그아웃/강제차단 시 `auth:revoke` 채널로 `{jti, exp}` 이벤트를 발행하면, Gateway가 Redis Pub/Sub을 통해 수신하여 로컬 메모리(DenySet)에 등록합니다. 이후 JWT 서명/만료 검증 후 `jti`가 DenySet에 있으면 즉시 401을 반환합니다(요청당 Redis 조회 없음).

- 사전 준비
  - 인프라: `docker compose up -d redis`
  - 키 파일: 개발용 placeholder가 포함되어 있으므로 필요 시 실제 키로 교체(auth/gateway `resources/keys`).

- 실행 순서
  1) 애플리케이션 기동: `auth(8081) → gateway(8080) → order(8082)`
  2) 회원가입/로그인
     ```bash
     curl -X POST http://localhost:8081/api/auth/signup \
          -H "Content-Type: application/json" \
          -d '{"username":"u1","password":"p1"}'

     # 로그인 → 응답 헤더 Authorization에 Access 토큰, 쿠키에 Refresh 저장
     curl -i -X POST http://localhost:8081/api/auth/login \
          -H "Content-Type: application/json" \
          -d '{"username":"u1","password":"p1"}'
     ```
  3) 보호 API 호출(성공)
     ```bash
     # 응답 헤더의 Authorization: Bearer {access} 값을 아래에 대입
     ACCESS=eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
     curl -H "Authorization: Bearer $ACCESS" \
          http://localhost:8080/api/order/products
     ```
  4) 로그아웃 → 토큰 즉시 차단
     ```bash
     curl -X POST http://localhost:8081/api/auth/logout \
          -H "Authorization: Bearer $ACCESS"
     ```
  5) 같은 Access로 다시 호출 → 401(게이트웨이 인메모리 차단)
     ```bash
     curl -i -H "Authorization: Bearer $ACCESS" \
          http://localhost:8080/api/order/products
     ```

- 참고 설정
  - Gateway `application.yml`
    ```yaml
    spring:
      data:
        redis:
          host: localhost
          port: 6379
    ```

### 2) 사가 오케스트레이션(보상 트랜잭션)
- 개요: `Order`에서 주문/재고 차감 후, `Worker`가 결제를 시뮬레이션합니다. 결제가 실패하면 `Worker`가 보상 커맨드 `StockRestoreCommand(orderId, productId, quantity)`를 발행하고, `Order`가 이를 수신하여 재고를 복구하고 주문 상태를 `CANCELED`로 변경합니다.

- 인프라/기동
  - 인프라: `docker compose up -d redis rabbitmq`
  - 앱: `auth(8081) → gateway(8080) → order(8082) → worker(8084)` 순으로 기동

- 결제 실패(보상 유도) 토글
  - `worker/src/main/resources/application.yml`에 다음 설정을 추가하거나 환경변수로 전달
    ```yaml
    worker:
      orchestration:
        fail-payment: true
    ```

- 시나리오
  1) 로그인하여 Access 준비(옵션 B 섹션 참고)
  2) 주문 생성(게이트웨이를 통해)
     ```bash
     curl -X POST http://localhost:8080/api/order \
          -H "Authorization: Bearer $ACCESS" \
          -H "Content-Type: application/json" \
          -d '{"userId":1, "productId":1, "quantity":2}'
     ```
  3) 기대 결과
     - Worker 로그: 결제 실패(데모) → `orders.stock.restore`로 보상 커맨드 발행
     - Order가 커맨드 수신 → 제품 재고 복원 + 주문 상태 `CANCELED`
  4) 확인 방법
     - Order 콘솔 로그 또는 H2 콘솔에서 `orders.status = 'CANCELED'` 확인
     - `products.stock`이 주문 이전 수량으로 복원되었는지 확인

- 사용된 메시지/리소스
  - 교환: `orders.exchange`
  - 큐: `orders.created.queue`(주문 생성), `orders.stock.restore.queue`(보상)
  - 라우팅키: `orders.created`, `orders.stock.restore`

### 트러블슈팅
- 게이트웨이 401이 즉시 되지 않는 경우
  - `auth/logout` 호출 시 Auth 로그에 `[Auth] Published revoke`가 찍히는지 확인
  - Gateway 로그에 `[Gateway] Subscribed to revoke channel` 및 `Revoked jti=...`가 찍히는지 확인
  - Redis가 정상 기동/접속되는지 확인(포트 6379)

- 보상 트랜잭션이 동작하지 않는 경우
  - RabbitMQ 기동 여부(5672), `orders.exchange` 바인딩 및 큐 존재 여부 확인
  - Worker 로그에서 보상 커맨드 발행 로그 확인, Order에서 보상 리스너 로그 확인
  - H2 메모리 DB 특성상 재기동 시 데이터가 초기화됩니다(시나리오 재실행 필요)
