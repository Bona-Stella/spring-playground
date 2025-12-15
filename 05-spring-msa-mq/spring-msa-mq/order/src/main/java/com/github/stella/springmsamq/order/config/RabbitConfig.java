package com.github.stella.springmsamq.order.config;

import com.github.stella.springmsamq.common.event.OrderAmqp;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    // 1️⃣ [우체국 허브 만들기] Exchange
    // 메시지를 처음 받는 '교환소'를 만듭니다.
    // TopicExchange는 메시지의 '주소(Routing Key)'를 보고 알맞은 큐로 뿌려주는 역할을 합니다.
    @Bean
    public TopicExchange ordersExchange() {
        // 첫 번째 인자: Exchange 이름 (예: "orders.exchange")
        // 두 번째 인자(true): durable - 서버가 재부팅돼도 이 교환소 설정을 유지함
        // 세 번째 인자(false): autoDelete - 사용하는 곳이 없어도 삭제하지 않음
        return new TopicExchange(OrderAmqp.EXCHANGE, true, false);
    }

    // 2️⃣ [번역기 설정] MessageConverter
    // 자바 객체(Java Object)를 메시지로 보낼 때, 사람이 읽기 쉬운 'JSON' 형식으로 바꿔줍니다.
    // 반대로 받을 때도 JSON을 자바 객체로 바꿔줍니다. (이게 없으면 알 수 없는 깨진 문자로 보일 수 있음)
    @Bean
    public Jackson2JsonMessageConverter producerMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // 3️⃣ [보내는 도구] RabbitTemplate
    // 실제로 메시지를 "발송"할 때 사용하는 도구입니다.
    // Service 로직에서 이 template을 주입받아 .convertAndSend()를 호출합니다.
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter producerMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        // 위에서 만든 JSON 번역기를 장착시킵니다.
        template.setMessageConverter(producerMessageConverter);
        return template;
    }

    // 4️⃣ [받는 도구 설정] ListenerContainerFactory
    // 메시지를 "수신(Listen)"할 때의 설정을 담당합니다.
    // @RabbitListener가 붙은 메서드가 메시지를 받을 때, 이 설정을 따릅니다.
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory,
                                                                               Jackson2JsonMessageConverter producerMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        // 받을 때도 JSON 번역기를 써서 자바 객체로 변환하게 합니다.
        factory.setMessageConverter(producerMessageConverter);
        return factory;
    }

    // ---------------- [실제 비즈니스 로직 연결] ----------------

    // 5️⃣ [우편함 만들기] Queue
    // "재고 복원(Stock Restore)" 메시지만 쌓아두는 전용 우편함을 만듭니다.
    // 주문 실패 시 "재고 다시 채워놔!" 라는 쪽지가 여기에 들어옵니다.
    @Bean
    public Queue stockRestoreQueue() {
        // durable: 서버가 꺼져도 우편함 안의 메시지를 날리지 않고 기억함 (안전장치)
        return QueueBuilder.durable(OrderAmqp.QUEUE_STOCK_RESTORE).build();
    }

    // 6️⃣ [배달 규칙 연결] Binding
    // 교환소(Exchange)와 우편함(Queue)을 연결해줍니다.
    // 규칙: "Exchange에 온 메시지 중, 주소(Routing Key)가 'stock.restore'인 것만 이 우편함에 넣어라"
    @Bean
    public Binding stockRestoreBinding(TopicExchange ordersExchange, Queue stockRestoreQueue) {
        return BindingBuilder
                .bind(stockRestoreQueue) // 이 우편함을
                .to(ordersExchange)      // 이 교환소에 연결하는데
                .with(OrderAmqp.ROUTING_KEY_STOCK_RESTORE); // 이 주소(Key)를 가진 편지만 받겠다.
    }
}
