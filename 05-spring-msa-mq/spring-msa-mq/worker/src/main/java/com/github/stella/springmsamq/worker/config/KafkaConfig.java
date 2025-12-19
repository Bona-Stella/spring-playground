package com.github.stella.springmsamq.worker.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class KafkaConfig {

    private final WorkerProperties props;

    public KafkaConfig(WorkerProperties props) {
        this.props = props;
    }

    /**
     * 1. ConsumerFactory 생성
     * - Kafka 브로커 접속 정보 및 메시지 직렬화 방식을 정의합니다.
     */
    @Bean
    public ConsumerFactory<String, Object> consumerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrap,
            @Value("${spring.kafka.consumer.group-id}") String groupId
    ) {
        Map<String, Object> props = new HashMap<>();

        // 브로커 주소 및 소비자 그룹 ID 바인딩
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);

        // Deserializer 설정: Key는 문자열, Value는 JSON 객체로 변환
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        // 신뢰할 수 있는 패키지 설정 (개발 편의성을 위해 전체 허용)
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");

        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * 2. KafkaListenerContainerFactory 생성
     * - 실제 메시지를 수신하는 리스너 컨테이너의 동작 방식(스레드, 동시성 등)을 제어합니다.
     */
    @Bean(name = "kafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);

        // [Virtual Threads 적용]
        // WorkerProperties 설정에 따라 Java 21 가상 스레드를 활성화하여
        // 대량의 메시지 처리 시 스레드 블로킹 비용을 최소화합니다.
        if (props.getVthreads().isEnabled()) {
            ExecutorService vThreads = Executors.newVirtualThreadPerTaskExecutor();
            AsyncTaskExecutor taskExecutor = new TaskExecutorAdapter(vThreads);
            factory.getContainerProperties().setListenerTaskExecutor(taskExecutor);
        }

        // [동시성 레벨 설정]
        // 메시징 처리량을 일관되게 관리하기 위해 RabbitMQ 설정에 정의된 concurrency 값을 공유하여 사용합니다.
        if (props.getRabbit().getConcurrency() > 0) {
            factory.setConcurrency(props.getRabbit().getConcurrency());
        }

        // 리스너가 예외를 던져 Ack를 보내지 못했을 때, 해당 메시지를 폐기(Discard) 처리합니다.
        // (필요 시 SeekToCurrentErrorHandler 등을 통해 재시도 로직 추가 가능)
        factory.setAckDiscarded(true);

        return factory;
    }
}
