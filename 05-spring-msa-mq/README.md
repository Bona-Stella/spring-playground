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
####🔹 Redis (In-Memory Performance)
- Caching: DB 조회 부하 감소 (Look-aside 패턴).
- Session Store: MSA 간 로그인 세션 공유.
- Distributed Lock: 재고 차감 등 동시성 제어.
- Pub/Sub: 실시간 웹소켓 메시지 브로드캐스팅.  
####🔹 RabbitMQ (Task Reliability)
- 용도: 반드시 처리되어야 하는 작업 (주문 접수, 이메일 발송).
- 특징: 메시지 확인(Ack), 재시도(Retry), 실패 격리(DLQ) 보장.  
####🔹 Apache Kafka (Data Streaming)
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



