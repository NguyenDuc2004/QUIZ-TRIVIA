package com.datn.quizai.realtime.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Nghe kênh Redis {@code room:*:events} rồi chuyển sự kiện xuống các client đang nối vào
 * <b>instance này</b> (FR-24).
 * <p>
 * Một mình lớp này khép kín phần scale ngang: thêm bao nhiêu instance backend cũng được,
 * instance nào giữ WebSocket của người chơi nào thì tự chuyển tiếp cho người đó.
 */
@Component
public class GameEventRelay implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(GameEventRelay.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public GameEventRelay(SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            GameEventPublisher.Envelope envelope = objectMapper.readValue(
                    new String(message.getBody(), StandardCharsets.UTF_8),
                    GameEventPublisher.Envelope.class);

            if (envelope.targetUserId() == null) {
                messagingTemplate.convertAndSend("/topic/room/" + envelope.roomCode(), envelope.event());
            } else {
                // Đích thật sự là /user/{userId}/queue/room/{code}; Spring tự ghép tiền tố
                messagingTemplate.convertAndSendToUser(
                        envelope.targetUserId().toString(),
                        "/queue/room/" + envelope.roomCode(),
                        envelope.event());
            }
        } catch (Exception e) {
            // Nuốt lỗi có chủ đích: một message hỏng không được làm chết listener của cả hệ thống
            log.error("Không chuyển tiếp được sự kiện phòng", e);
        }
    }
}
