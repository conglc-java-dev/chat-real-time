package com.example.wschat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
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
        // Broker quản lý kênh /topic (broadcast) và /queue (1-1)
        registry.enableSimpleBroker("/topic", "/queue"); // khoi tao broker voi cac tien to
        registry.setApplicationDestinationPrefixes("/app"); // goi tin co tien to /app => chuyen vao controller xu ly

        // Tiền tố dành riêng cho tin nhắn gửi tới từng user cụ thể
        // Mặc định Spring đã là "/user", khai báo rõ để tường minh
        registry.setUserDestinationPrefix("/user");
    }

    /**
     * ChannelInterceptor: Bắt sự kiện STOMP CONNECT, giống 1 lớp filter cắt ngang gói tin từ client gửi lên server trước khi vào controller
     * đọc header "username" do client gửi lên và lưu vào SessionAttributes.
     * WebSocketEventListener sẽ đọc lại từ SessionAttributes để biết username của session.
     * Để khi user bị mất mạng đột ngột, k thể gửi disconnect message => biết được user nào offline 
     
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String username = accessor.getFirstNativeHeader("username");
                    if (username != null && !username.isBlank()) {
                        // Lưu username vào session để dùng lại ở Event Listener
                        accessor.getSessionAttributes().put("username", username);
                    }
                }
                return message;
            }
        });
    }
}
