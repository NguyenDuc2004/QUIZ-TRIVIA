package com.datn.quizai.config;

import com.datn.quizai.realtime.security.StompAuthChannelInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Cấu hình WebSocket/STOMP cho phòng đấu (docs/api.md §5.2).
 * <p>
 * Dùng <b>simple broker trong bộ nhớ</b> chứ không phải broker ngoài (RabbitMQ đã bị loại khỏi
 * stack). Muốn chạy nhiều instance vẫn đồng bộ được thì mọi thông điệp đi vòng qua Redis Pub/Sub
 * trước — xem {@code GameEventPublisher} và {@code GameEventRelay}.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor authInterceptor;
    private final String allowedOrigins;

    public WebSocketConfig(StompAuthChannelInterceptor authInterceptor,
                           @Value("${app.cors.allowed-origins}") String allowedOrigins) {
        this.authInterceptor = authInterceptor;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                // Cùng lý do với CORS ở SecurityConfig: khi dev, điện thoại mở frontend qua IP LAN
                // nên không liệt kê trước được origin — phải dùng mẫu.
                .setAllowedOriginPatterns(allowedOrigins.split(","))
                // SockJS để trình duyệt/mạng chặn WebSocket vẫn chơi được (fallback long-polling)
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // /topic: phát cho cả phòng · /queue: gửi riêng một người (kết quả câu trả lời)
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Xác thực JWT ngay ở frame CONNECT, trước khi cho subscribe bất cứ kênh nào
        registration.interceptors(authInterceptor);
    }
}
