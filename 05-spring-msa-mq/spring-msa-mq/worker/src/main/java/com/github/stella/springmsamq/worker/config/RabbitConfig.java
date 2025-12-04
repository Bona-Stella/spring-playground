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

    @Bean
    public TopicExchange ordersExchange() {
        return new TopicExchange(OrderAmqp.EXCHANGE, true, false);
    }

    @Bean
    public Queue ordersCreatedQueue() {
        return QueueBuilder.durable(OrderAmqp.QUEUE_CREATED)
                .withArgument("x-dead-letter-exchange", "orders.dlx")
                .withArgument("x-dead-letter-routing-key", OrderAmqp.ROUTING_KEY_CREATED)
                .build();
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
        // Virtual Threads 토글
        if (props.getVthreads().isEnabled()) {
            ExecutorService vThreads = Executors.newVirtualThreadPerTaskExecutor();
            factory.setTaskExecutor(new TaskExecutorAdapter(vThreads));
        }
        // 동시성 설정
        factory.setConcurrentConsumers(Math.max(1, props.getRabbit().getConcurrency()));
        factory.setDefaultRequeueRejected(false); // send to DLQ on exception
        return factory;
    }

    // Dead Letter Exchange/Queue
    @Bean
    public DirectExchange ordersDlx() {
        return new DirectExchange("orders.dlx", true, false);
    }

    @Bean
    public Queue ordersCreatedDlq() {
        return QueueBuilder.durable("orders.created.dlq").build();
    }

    @Bean
    public Binding ordersCreatedDlqBinding(DirectExchange ordersDlx, Queue ordersCreatedDlq) {
        return BindingBuilder.bind(ordersCreatedDlq).to(ordersDlx).with(OrderAmqp.ROUTING_KEY_CREATED);
    }

    // Retry Queue (TTL 후 메인으로 재유입)
    @Bean
    public Queue ordersCreatedRetryQueue() {
        return QueueBuilder.durable(OrderAmqp.QUEUE_CREATED_RETRY)
                .withArgument("x-dead-letter-exchange", OrderAmqp.EXCHANGE)
                .withArgument("x-dead-letter-routing-key", OrderAmqp.ROUTING_KEY_CREATED)
                .withArgument("x-message-ttl", props.getRetry().getTtl())
                .build();
    }
}
