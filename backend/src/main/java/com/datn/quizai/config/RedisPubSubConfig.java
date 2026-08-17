package com.datn.quizai.config;

import com.datn.quizai.notification.service.NotificationPusher;
import com.datn.quizai.notification.service.NotificationRelay;
import com.datn.quizai.realtime.service.GameEventRelay;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Đăng ký nghe Redis Pub/Sub cho sự kiện phòng đấu (FR-24) và thông báo real-time (FR-67).
 * <p>
 * Cả hai đi cùng một lối vì cùng một lý do: broker STOMP của Spring nằm trong bộ nhớ <i>từng</i> instance, nên
 * instance nào giữ WebSocket của ai thì phải chính nó chuyển tiếp. Redis là chỗ mọi instance cùng nghe.
 * <p>
 * Sự kiện phòng nghe theo <b>mẫu</b> {@code room:*:events} thay vì subscribe từng phòng: mở/đóng phòng liên
 * tục mà phải subscribe/unsubscribe theo thì vừa rườm rà vừa dễ sót. Thông báo thì chỉ <b>một kênh cố định</b>
 * — người nhận nằm trong gói tin, không nằm trong tên kênh, nên không cần mẫu.
 */
@Configuration
public class RedisPubSubConfig {

    /** Khớp mọi kênh sự kiện phòng — xem {@code GameEventPublisher.channel}. */
    private static final String ROOM_EVENT_PATTERN = "room:*:events";

    /**
     * Một container cho cả hai listener, không phải hai container.
     * <p>
     * Mỗi {@code RedisMessageListenerContainer} tự dựng một bể luồng riêng và giữ một kết nối riêng. Hai cái
     * cho hai kênh là trả giá hai lần cho cùng một việc.
     */
    @Bean
    RedisMessageListenerContainer redisListenerContainer(RedisConnectionFactory connectionFactory,
                                                        GameEventRelay roomRelay,
                                                        NotificationRelay notificationRelay) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(roomRelay, new PatternTopic(ROOM_EVENT_PATTERN));
        container.addMessageListener(notificationRelay, new ChannelTopic(NotificationPusher.CHANNEL));
        return container;
    }
}
