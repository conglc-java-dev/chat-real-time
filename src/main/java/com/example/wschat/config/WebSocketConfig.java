package com.example.wschat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")  // client gui http endpoint /ws de yeu cau backend nang len giao thuc websocket
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic"); // bat trinh dieu phoi tin nhan, quan ly cac kenh co prefix /topic
        registry.setApplicationDestinationPrefixes("/app");   // dat tien to cho goi tin ma clien muon gui cho backend
        // client gui : /app/chat.send -> di vao controller tim ham co anotation /chat.send
    }
}
