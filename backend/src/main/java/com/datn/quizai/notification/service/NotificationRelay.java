package com.datn.quizai.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Nghe kênh Redis {@code notifications} rồi chuyển thông báo xuống client đang nối vào <b>instance này</b>
 * (features/16, FR-67).
 * <p>
 * Đích thật sự là {@code /user/{userId}/queue/notifications}; Spring tự ghép tiền tố {@code /user} theo
 * {@code Principal.getName()}. Khoá tìm phiên là <b>chuỗi UUID của người dùng</b> — đúng vì
 * {@code RoomParticipant} cài {@code AuthenticatedPrincipal} và trả {@code playerId} làm tên. Nếu chỗ đó đổi
 * thì thông báo real-time lặng lẽ không tới ai, nên đây là một ràng buộc giữa hai tính năng.
 * <p>
 * Người dùng không online thì {@code convertAndSendToUser} không tìm thấy phiên nào và <b>không</b> báo lỗi.
 * Đúng như mong muốn: thông báo đã ở trong cơ sở dữ liệu, họ sẽ thấy ở lần mở trang sau.
 */
@Component
public class NotificationRelay implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationRelay.class);

    /** Đích của client sau tiền tố {@code /user} — khớp với chỗ frontend subscribe. */
    public static final String DESTINATION = "/queue/notifications";

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public NotificationRelay(SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            NotificationPusher.Envelope envelope = objectMapper.readValue(
                    new String(message.getBody(), StandardCharsets.UTF_8),
                    NotificationPusher.Envelope.class);

            messagingTemplate.convertAndSendToUser(
                    envelope.userId().toString(), DESTINATION, envelope.notification());

        } catch (Exception e) {
            // Nuốt có chủ đích: một message hỏng không được làm chết listener của cả hệ thống
            log.error("Không chuyển tiếp được thông báo", e);
        }
    }
}
