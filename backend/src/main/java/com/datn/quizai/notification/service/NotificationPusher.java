package com.datn.quizai.notification.service;

import com.datn.quizai.notification.domain.Notification;
import com.datn.quizai.notification.dto.NotificationResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Đẩy thông báo xuống người dùng đang online (features/16, FR-67).
 *
 * <h3>Đi vòng qua Redis, không gửi thẳng vào broker</h3>
 * Broker STOMP của Spring nằm trong bộ nhớ <i>từng</i> instance. Instance A tạo thông báo mà người dùng đang
 * giữ WebSocket ở instance B thì gửi thẳng là gửi vào chỗ không có ai. Nên đi đúng con đường mà phòng đấu đã
 * dùng: publish lên Redis, mọi instance cùng nghe, {@link NotificationRelay} chuyển tiếp xuống client của
 * riêng mình. Instance vừa publish cũng nhận lại chính message đó — đúng như mong muốn, vì đây là đường duy
 * nhất tới broker cục bộ.
 *
 * <h3>Đẩy thất bại KHÔNG được làm vỡ việc tạo thông báo</h3>
 * Thông báo đã nằm trong cơ sở dữ liệu, người dùng sẽ thấy nó ở lần mở trang sau. Real-time là phần thêm cho
 * người đang online, không phải điều kiện để thông báo tồn tại. Redis chết mà cả luồng nộp bài đổ theo thì
 * cái giá lớn hơn cái được nhiều lần.
 */
@Service
public class NotificationPusher {

    private static final Logger log = LoggerFactory.getLogger(NotificationPusher.class);

    /** Kênh Pub/Sub — {@link NotificationRelay} nghe đúng tên này. */
    public static final String CHANNEL = "notifications";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public NotificationPusher(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public void day(UUID userId, Notification notification) {
        try {
            Envelope envelope = new Envelope(userId, NotificationResponse.from(notification));
            redis.convertAndSend(CHANNEL, objectMapper.writeValueAsString(envelope));
        } catch (Exception e) {
            log.warn("Không đẩy được thông báo real-time cho {}: {}", userId, e.getMessage());
        }
    }

    /** Gói tin trên kênh Redis: ai nhận, và nội dung đã ở dạng client đọc được. */
    public record Envelope(UUID userId, NotificationResponse notification) {
    }
}
