# 📌 05 — spring-live-commerce-lab

## 🚀 개요
메시지 큐(RabbitMQ), Redis(Pub/Sub, Lock), 비동기 처리(@Async + Virtual Threads) 기술을 총망라하여 **"고동시성 라이브 커머스 시스템"**을 구축 및 실험하는 프로젝트입니다.

주요 실험 포인트:
알림 발송: 단순 @Async와 RabbitMQ의 안정성 차이 비교 (서버 강제 종료 테스트)
라이브 채팅: WebSocket과 Redis Pub/Sub을 결합한 멀티 인스턴스 채팅 동기화
선착순 구매: Redis 분산 락과 MQ를 이용한 트래픽 제어(Backpressure) 구현
DIY 큐: Redis List + 가상 스레드를 활용한 간이 큐 구현과 한계점 분석

## 🧱 아키텍처 및 기술 구조
### 🔹 Redis 활용 구조
```
[Chatting Service]
User A (WS) → Server 1 
User B (WS) → Server 2
     ↘
      Redis Pub/Sub (Topic: live_room_1)
     ↙
Server 1, 2 (Subscribe & Broadcast)
```
```
[Inventory Lock]
Order Service
 → Redis SET NX PX (key: item_100_lock)
     → 성공: 재고 차감 로직 수행
     → 실패: 잠시 대기 (Spin Lock) or 실패 응답
```
```
[Simple Job Queue (DIY)]
Producer
 → Redis LPUSH (key: email_queue)
Consumer (Virtual Threads)
 → Redis BRPOP (Blocking Read)
```
🔹 메시지 큐 (RabbitMQ) 구조
```
[Order Buffer]
Controller → Exchange (Direct) → Queue (order_queue)
                                      ↓
                               Consumer (Virtual Threads)
                                 → DB Transaction
                                 → Ack / Nack
                                 → DLQ (Dead Letter Queue - 실패 시)
```
## 🔄 동기 / 비동기 / MQ 워크플로우 설계
### 🔹 시나리오 1: 회원 가입 축하 메일 (비교 실험)
#### Case A: Spring @Async (가상 스레드)
```
Controller
  → Service.join() (DB 저장)
  → @Async MailService.send() (메모리에서 실행)
      → (실험: 이 시점에 서버 강제 종료 시 메일 유실 확인)
```
#### Case B: RabbitMQ
```
Controller
  → Service.join()
  → RabbitTemplate.convertAndSend() (MQ에 저장)
      → (서버 재시작)
      → Consumer가 MQ에서 메시지 수신 후 메일 발송 (유실 없음 확인)
```
### 🔹 시나리오 2: 라이브 방송 채팅 (실시간성)
```
Client (WebSocket)
  → StompHandler (Interceptor)
      → Redis Publisher
          → Redis Channel (Topic)
      → Redis Subscriber (Listener)
          → SimpMessageSendingOperations
              → 구독자 전원에게 메시지 전송
```
### 🔹 시나리오 3: 선착순 100개 한정 판매 (트래픽 제어)
Flow (Redis Lock + MQ 조합)
```
User Request
  → Controller
    → Redis 분산 락 획득 (재고 조회 동시성 제어)
      → 재고 있음 확인
        → RabbitMQ에 "주문 생성 요청" 발행 (빠른 응답)
    → 락 해제
  → 사용자에게 "주문 대기 중" 응답

Async Consumer
  → MQ에서 주문 꺼냄
  → DB 결제 처리 및 실제 재고 차감 (최종 일관성)
  → 사용자에게 "주문 완료" 알림 (WebSocket or Push)
```
## 🔍 개발 순서 및 실습 상세 (Roadmap)
### Step 1. 환경 설정 및 기본 비동기 (@Async)
목표: Java 21 Virtual Threads 활성화 및 @Async 동작 확인.
구현: 회원 가입 시 3초 걸리는 이메일 발송을 @Async로 처리.
실험: 메일 발송 중 kill -9로 서버 종료 시 로그가 남는지 확인 (실패 유도).
### Step 2. Redis Pub/Sub 채팅 서버
목표: 서버가 2대 떠 있을 때도 대화가 되는지 확인.
구현: RedisMessageListenerContainer 적용, STOMP 프로토콜 연동.
실험: 8080 포트, 8081 포트로 각각 접속한 유저끼리 대화 가능 여부 테스트.
### Step 3. RabbitMQ 연동 및 신뢰성 확보
목표: Step 1의 이메일 유실 문제를 MQ로 해결.
구현: RabbitMQ 컨테이너 띄우기, Producer/Consumer 구현, ACK 모드 설정.
실험: 메시지 발행 후 Consumer 서버를 껐다 켜도 메시지가 처리되는지(Persistence) 확인.
### Step 4. 고동시성 선착순 주문 (The Final Boss)
목표: 재고 100개에 1,000명이 몰릴 때 정확히 100개만 팔려야 함.
구현:
DB Lock (Pessimistic) 만 썼을 때의 성능 측정.
Redis 분산 락 (Redisson) 적용 후 성능 비교.
MQ를 도입하여 DB 부하를 줄이는 아키텍처 완성.
## 📦 핵심 코드 예시 (RabbitMQ + Virtual Thread Consumer)
### RabbitMQ Config (Virtual Thread Listener)
```java
@Bean
public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    factory.setConnectionFactory(connectionFactory);
    // 컨슈머 처리에 자바 21 가상 스레드 사용 (높은 처리량)
    factory.setTaskExecutor(new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor()));
    factory.setAcknowledgeMode(AcknowledgeMode.MANUAL); // 수동 Ack
    factory.setPrefetchCount(50); // 한 번에 가져올 메시지 수
    return factory;
}
```
### Consumer (주문 처리)
```java
@RabbitListener(queues = "order.queue")
public void receiveMessage(OrderMessage message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
    try {
        // 가상 스레드 위에서 실행됨
        orderService.processOrder(message);
        
        // 성공 시 Ack (메시지 삭제)
        channel.basicAck(tag, false);
    } catch (Exception e) {
        // 실패 시 Nack (DLQ로 보낼지, 재시도할지 결정)
        channel.basicNack(tag, false, false); 
    }
}
```
## 📦 공통 Response, Error 템플릿
- API Success Response Specification.md 참고
- Error Response Specification.md 참고



