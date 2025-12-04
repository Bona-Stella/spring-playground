package com.github.stella.springmsamq.worker.config;

import com.github.stella.springmsamq.common.event.OrderAmqp;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    public TopicExchange ordersExchange() {
        return new TopicExchange(OrderAmqp.EXCHANGE, true, false);
    }

    @Bean
    public Queue ordersCreatedQueue() {
        return QueueBuilder.durable(OrderAmqp.QUEUE_CREATED).build();
    }

    @Bean
    public Binding ordersCreatedBinding(TopicExchange ordersExchange, Queue ordersCreatedQueue) {
        return BindingBuilder.bind(ordersCreatedQueue)
                .to(ordersExchange)
                .with(OrderAmqp.ROUTING_KEY_CREATED);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory,
                                                                              Jackson2JsonMessageConverter converter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(converter);
        return factory;
    }
}
