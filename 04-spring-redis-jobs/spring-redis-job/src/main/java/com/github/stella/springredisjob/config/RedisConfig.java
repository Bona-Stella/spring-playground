package com.github.stella.springredisjob.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        // 1. RedisTemplate 객체 생성
        // StringRedisTemplate과 달리, Key는 String이지만 Value는 모든 객체(Object)를 담을 수 있게 설정함
        RedisTemplate<String, Object> template = new RedisTemplate<>();

        // application.yml에 설정한 Redis 접속 정보(host, port 등)가 담긴 factory를 주입받아 연결
        template.setConnectionFactory(factory);

        // 역할: 자바의 String을 Redis의 일반 문자열로 저장
        // 효과: Redis에서 키를 조회할 때 "\xac\xed..." 같은 깨진 문자가 아니라 "user:100" 처럼 깔끔하게 보임
        StringRedisSerializer keySerializer = new StringRedisSerializer();

        // [Value용] GenericJackson2JsonRedisSerializer
        // 역할: 자바의 객체(Object)를 JSON 형태의 문자열로 변환하여 저장
        // 효과:
        //   1) 객체에 'implements Serializable'을 하지 않아도 됨 (serialVersionUID 불필요)
        //   2) Redis에 '{"name":"bini", "age":20, "@class":"..."}' 형태로 저장되어 눈으로 확인 가능
        GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer();

        // 4. 단일 Key:Value 구조에 대한 직렬화 설정 (opsForValue)
        template.setKeySerializer(keySerializer);
        template.setValueSerializer(valueSerializer);

        // 5. Hash 자료구조(Key:Field:Value)에 대한 직렬화 설정 (opsForHash)
        // Hash를 사용할 때도 똑같이 키는 문자열, 값은 JSON으로 다루겠다는 의미
        template.setHashKeySerializer(keySerializer);
        template.setHashValueSerializer(valueSerializer);

        // 6. 설정 완료 및 초기화
        // 설정한 시리얼라이저들이 정상인지 검사하고 템플릿을 사용할 준비를 마침
        template.afterPropertiesSet();
        return template;
    }

    /* Session 전용 직렬화
    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        return new GenericJackson2JsonRedisSerializer();
    }
    */

    /* Custom 해야한다면 추가 개발. 굳이 명시할 필요 없음.
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }
    */

    /*
    이 빈(Bean)을 등록함으로써, 이제 서비스 코드에서
    `@Cacheable(value = "ranks")` 한 줄만 적으면:
    결과가 JSON으로 변환되어 Redis에 저장되고,
    Key 앞에 cache:ranks: 가 붙어서 관리하기 편해지고,
    5분 뒤에 알아서 사라지게 됩니다.
    */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        // 1. 직렬화(변환기) 도구 준비
        // 캐시 데이터를 사람이 읽을 수 있는 JSON 포맷으로 저장하기 위함
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
        StringRedisSerializer keySerializer = new StringRedisSerializer();

        // 2. 캐시 설정 구성 (House Rules)
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                // [중요] 모든 캐시의 기본 유효시간을 5분으로 설정
                // 5분이 지나면 Redis에서 자동으로 삭제됨
                .entryTtl(Duration.ofMinutes(5))
                // 메서드 리턴값이 null이면 캐싱하지 않음 (메모리 아끼기)
                .disableCachingNullValues()
                // [키 이름 규칙 커스텀]
                // 기본값(::) 대신 "cache:캐시이름:키" 형태로 저장됨
                // 예: @Cacheable(value="user", key="1") -> "cache:user:1"
                .computePrefixWith(cacheName -> "cache:" + cacheName + ":")
                // [직렬화 적용] Key는 문자열로 저장
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(keySerializer))
                // [직렬화 적용] Value는 JSON으로 저장 (이게 없으면 외계어로 저장됨)
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer));

        // 3. 설정이 적용된 CacheManager 생성 및 반환
        return RedisCacheManager.builder(factory)
                .cacheDefaults(config)
                .build();
    }


    /*
    keyGenerator 속성에 빈 이름을 적어줍니다.
    @Cacheable(value = "posts", keyGenerator = "simpleKeyGenerator")
    */
    @Bean
    public KeyGenerator simpleKeyGenerator() {
        // 람다식으로 작성된 KeyGenerator 구현체
        // target: 메서드를 호출한 객체 (Service 등)
        // method: 호출된 메서드 정보
        // params: 메서드에 넘겨진 파라미터들 (인자값)
        return (target, method, params) -> {
            StringBuilder sb = new StringBuilder();
            // 1. 클래스 이름 추가 (예: BoardService)
            // getClass().getSimpleName()을 써서 패키지명 빼고 깔끔하게 클래스명만 가져옴
            sb.append(target.getClass().getSimpleName()).append(":");
            // 2. 메서드 이름 추가 (예: getPost)
            sb.append(method.getName());
            // 3. 파라미터 값들 추가 (예: 100, "user")
            for (Object param : params) {
                sb.append(":").append(param);
            }
            // 최종 결과 반환: "BoardService:getPost:100"
            return sb.toString();
        };
    }

    //  Redis Pub/Sub(발행/구독) 시스템에서 사용할 "채널(주파수) 이름"을 미리 정의해두는 빈(Bean)
    // Pub/Sub beans (topic name: notify)
    @Bean
    public PatternTopic notifyTopic() {
        return new PatternTopic("notify");
    }

    // Additional topic for external mock requests
    @Bean
    public PatternTopic externalRequestTopic() {
        return new PatternTopic("ext:requests");
    }

    // Topic for internal post events
    @Bean
    public PatternTopic postEventsTopic() {
        return new PatternTopic("post:events");
    }

    @Bean
    public MessageListenerAdapter messageListenerAdapter(com.github.stella.springredisjob.pubsub.NotifySubscriber subscriber) {
        // delegate method name 'handleMessage'
        // subscriber: 실제로 일을 처리할 자바 객체 (우리가 만든 빈)
        // "handleMessage": 메시지가 오면 실행할 메서드 이름 (문자열로 지정)
        return new MessageListenerAdapter(subscriber, "handleMessage");
    }

    // Listener adapter for external requests subscriber
    @Bean
    public MessageListenerAdapter externalRequestListenerAdapter(
            com.github.stella.springredisjob.external.mq.ExternalRequestSubscriber subscriber
    ) {
        return new MessageListenerAdapter(subscriber, "handleMessage");
    }

    // Listener adapter for post events subscriber
    @Bean
    public MessageListenerAdapter postEventListenerAdapter(
            com.github.stella.springredisjob.domain.post.PostEventSubscriber subscriber
    ) {
        return new MessageListenerAdapter(subscriber, "handleMessage");
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory factory,
            MessageListenerAdapter messageListenerAdapter,
            PatternTopic notifyTopic,
            MessageListenerAdapter externalRequestListenerAdapter,
            PatternTopic externalRequestTopic,
            MessageListenerAdapter postEventListenerAdapter,
            PatternTopic postEventsTopic,
            @Qualifier("applicationTaskExecutor") TaskExecutor applicationTaskExecutor
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);
        // spring.threads.virtual.enabled=true로 생성된 가상 스레드 TaskExecutor 사용
        container.setTaskExecutor(applicationTaskExecutor);
        container.addMessageListener(messageListenerAdapter, notifyTopic);
        container.addMessageListener(externalRequestListenerAdapter, externalRequestTopic);
        container.addMessageListener(postEventListenerAdapter, postEventsTopic);
        return container;
    }
}
