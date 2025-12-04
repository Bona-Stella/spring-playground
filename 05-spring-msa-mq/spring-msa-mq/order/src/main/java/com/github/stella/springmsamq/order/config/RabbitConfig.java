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

    @Bean
    public TopicExchange ordersExchange() {
        return new TopicExchange(OrderAmqp.EXCHANGE, true, false);
    }

    @Bean
    public Jackson2JsonMessageConverter producerMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter producerMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(producerMessageConverter);
        return template;
    }

    // Listener에서 JSON 역직렬화를 위한 컨테이너 팩토리
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory,
                                                                              Jackson2JsonMessageConverter producerMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(producerMessageConverter);
        return factory;
    }

    // 보상(Compensation)용 큐/바인딩: 재고 복원 커맨드
    @Bean
    public Queue stockRestoreQueue() {
        return QueueBuilder.durable(OrderAmqp.QUEUE_STOCK_RESTORE).build();
    }

    @Bean
    public Binding stockRestoreBinding(TopicExchange ordersExchange, Queue stockRestoreQueue) {
        return BindingBuilder.bind(stockRestoreQueue).to(ordersExchange).with(OrderAmqp.ROUTING_KEY_STOCK_RESTORE);
    }
}
