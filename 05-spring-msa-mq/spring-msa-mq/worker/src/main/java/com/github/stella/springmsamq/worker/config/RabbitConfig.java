package com.github.stella.springmsamq.worker.config;

import com.github.stella.springmsamq.common.event.OrderAmqp;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.support.TaskExecutorAdapter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class RabbitConfig {

    private final WorkerProperties props;

    public RabbitConfig(WorkerProperties props) {
        this.props = props;
    }

    // =================================================================================
    // 1. Main Exchange & Queue (주문 처리 메인 경로)
    // =================================================================================

    /**
     * Topic Exchange 생성.
     * - Order 서비스가 메시지를 발행하는 교환소(Exchange)입니다.
     * - Routing Key 패턴에 따라 여러 큐로 메시지를 분기할 수 있습니다.
     */
    @Bean
    public TopicExchange ordersExchange() {
        // durable=true, autoDelete=false
        return new TopicExchange(OrderAmqp.EXCHANGE, true, false);
    }

    /**
     * 주문 생성 이벤트 수신 큐 (Main Queue).
     * - 이 큐에서 예외가 발생(Nack/Reject)하면 메시지는 버려지지 않고 DLX로 이동합니다.
     */
    @Bean
    public Queue ordersCreatedQueue() {
        return QueueBuilder.durable(OrderAmqp.QUEUE_CREATED)
                // [DLQ 설정] 처리 실패 시 이동할 Exchange 지정
                .withArgument("x-dead-letter-exchange", "orders.dlx")
                // [DLQ 설정] 이동 시 사용할 Routing Key (원본 키 유지 또는 변경)
                .withArgument("x-dead-letter-routing-key", OrderAmqp.ROUTING_KEY_CREATED)
                .build();
    }

    /**
     * 메인 큐와 Exchange 바인딩.
     * - 특정 Routing Key(예: order.created)를 가진 메시지만 이 큐로 들어오게 연결합니다.
     */
    @Bean
    public Binding ordersCreatedBinding(TopicExchange ordersExchange, Queue ordersCreatedQueue) {

        return BindingBuilder.bind(ordersCreatedQueue)     // 1. 목적지: 이 큐로 보내주세요.
                .to(ordersExchange)                         // 2. 출발지: 이 익스체인지에서 온 것 중에서요.
                .with(OrderAmqp.ROUTING_KEY_CREATED);    // 3. 규칙: 라우팅 키가 이거랑 똑같은 것만요.
    }

    // =================================================================================
    // 2. Listener Container & Converter (처리 환경 설정)
    // =================================================================================

    /**
     * JSON 메시지 컨버터.
     * - RabbitMQ의 바이트 메시지를 Java 객체(DTO)로 자동 변환합니다.
     */
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitMQ 리스너 컨테이너 팩토리.
     * - @RabbitListener가 실행될 환경(스레드, 에러 처리 방식 등)을 설정합니다.
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory,
                                                                               Jackson2JsonMessageConverter converter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(converter);

        // [Virtual Threads 적용]
        // WorkerProperties 설정에 따라 가상 스레드를 활성화하여 높은 동시성 처리를 지원합니다.
        if (props.getVthreads().isEnabled()) {
            ExecutorService vThreads = Executors.newVirtualThreadPerTaskExecutor();
            factory.setTaskExecutor(new TaskExecutorAdapter(vThreads));
        }

        // [동시성 설정]
        // 동시에 몇 개의 메시지를 처리할지 설정 (Consumer 스레드 수)
        factory.setConcurrentConsumers(Math.max(1, props.getRabbit().getConcurrency()));

        // [에러 처리 전략]
        // false로 설정해야 예외 발생 시 무한 재시도(Requeue)를 하지 않고,
        // 설정된 DLX(x-dead-letter-exchange)로 메시지를 보냅니다.
        factory.setDefaultRequeueRejected(false);

        return factory;
    }

    // =================================================================================
    // 3. Dead Letter Exchange/Queue (실패 처리 저장소)
    // =================================================================================

    /**
     * Dead Letter Exchange (DLX).
     * - 처리 실패한 메시지들이 최종적으로 도착하는 교환소입니다.
     */
    @Bean
    public DirectExchange ordersDlx() {
        return new DirectExchange("orders.dlx", true, false);
    }

    /**
     * Dead Letter Queue (DLQ).
     * - 최종 실패한 메시지가 저장되는 큐입니다. (추후 수동 확인 및 재처리 용도)
     */
    @Bean
    public Queue ordersCreatedDlq() {
        return QueueBuilder.durable("orders.created.dlq").build();
    }

    @Bean
    public Binding ordersCreatedDlqBinding(DirectExchange ordersDlx, Queue ordersCreatedDlq) {
        return BindingBuilder.bind(ordersCreatedDlq).to(ordersDlx).with(OrderAmqp.ROUTING_KEY_CREATED);
    }

    // =================================================================================
    // 4. Retry Queue (지연 재시도 로직)
    // =================================================================================

    /**
     * 재시도 대기 큐 (Delay Queue).
     * - TTL(Time To Live)이 설정되어 있어, 메시지가 일정 시간(예: 5초) 동안 머무른 후
     *   다시 메인 Exchange로 돌아가 재처리를 시도하게 합니다.
     * - 주의: 현재 이 큐로 메시지를 보내는 바인딩(Binding)은 명시되어 있지 않으므로,
     *   필요 시 Listener에서 명시적으로 전송하거나 별도 바인딩 추가가 필요합니다.
     */
    @Bean
    public Queue ordersCreatedRetryQueue() {
        return QueueBuilder.durable(OrderAmqp.QUEUE_CREATED_RETRY)
                // [TTL 만료 후 이동 경로] 다시 메인 Exchange로 보냄
                .withArgument("x-dead-letter-exchange", OrderAmqp.EXCHANGE)
                .withArgument("x-dead-letter-routing-key", OrderAmqp.ROUTING_KEY_CREATED)
                // [대기 시간] WorkerProperties에 설정된 시간만큼 대기
                .withArgument("x-message-ttl", props.getRetry().getTtl())
                .build();
    }
}
