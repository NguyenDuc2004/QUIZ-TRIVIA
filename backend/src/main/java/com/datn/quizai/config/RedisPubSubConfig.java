package com.datn.quizai.config;

import com.datn.quizai.realtime.service.GameEventRelay;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Đăng ký nghe Redis Pub/Sub cho sự kiện phòng đấu (FR-24).
 * <p>
 * Nghe theo <b>mẫu</b> {@code room:*:events} thay vì subscribe từng phòng: mở/đóng phòng liên tục
 * mà phải subscribe/unsubscribe theo thì vừa rườm rà vừa dễ sót, trong khi một pattern là đủ.
 */
@Configuration
public class RedisPubSubConfig {

    /** Khớp mọi kênh sự kiện phòng — xem {@code GameEventPublisher.channel}. */
    private static final String ROOM_EVENT_PATTERN = "room:*:events";

    @Bean
    RedisMessageListenerContainer roomEventListenerContainer(RedisConnectionFactory connectionFactory,
                                                             GameEventRelay relay) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(relay, new PatternTopic(ROOM_EVENT_PATTERN));
        return container;
    }
}
