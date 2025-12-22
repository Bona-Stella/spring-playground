package com.github.stella.springmsamq.chat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
// STOMP 프로토콜을 사용하여 메시지 브로커 기능을 활성화합니다.
// 일반 WebSocket은 단순 통로일 뿐이지만, STOMP를 쓰면 "구독(Sub)", "발행(Pub)" 같은 메시지 규격을 사용할 수 있습니다.
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * 1. 연결(Handshake) 설정
     * 클라이언트(프론트엔드)가 웹소켓 서버에 처음 접속할 때 사용하는 문(Door)입니다.
     * 예: var socket = new WebSocket("ws://localhost:8080/ws/chat");
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/chat") // 접속 주소(Endpoint) 설정
                .setAllowedOriginPatterns("*"); // CORS 설정: 모든 도메인에서 접속 허용 (보안상 추후 프론트 도메인만 지정하는 것이 좋음)
        // .withSockJS(); // 순수 WebSocket을 지원하지 않는 구형 브라우저를 위한 설정 (현재는 주석 처리됨 -> 순수 WebSocket만 사용)
    }

    /**
     * 2. 메시지 라우팅(Routing) 설정
     * 메시지가 들어오고 나가는 길(Prefix)을 정의합니다.
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {

        // [Client -> Server] (보낼 때)
        // 클라이언트가 메시지를 보낼 때 붙여야 하는 주소의 앞부분(Prefix)입니다.
        // 클라이언트가 "/pub/chat/message"로 보내면 -> 스프링이 @MessageMapping("/chat/message")가 달린 컨트롤러 메서드로 연결해줍니다.
        registry.setApplicationDestinationPrefixes("/pub");

        // [Server -> Client] (받을 때/구독할 때)
        // 클라이언트가 메시지를 받기 위해 듣고 있는(Subscribe) 주소의 앞부분입니다.
        // 스프링 내부의 'SimpleBroker'(메모리 브로커)를 켭니다.
        // 즉, 서버가 "/topic/room1"로 메시지를 쏘면 -> "/topic/room1"을 구독 중인 모든 클라이언트에게 전달됩니다.
        // 주의: 'Simple'은 메모리 기반이므로, 서버가 여러 대일 경우 이 설정만으로는 서로 다른 서버에 붙은 유저끼리 대화가 안 됩니다. (그래서 Redis Bridge가 필요함)
        registry.enableSimpleBroker("/topic");
    }
}
