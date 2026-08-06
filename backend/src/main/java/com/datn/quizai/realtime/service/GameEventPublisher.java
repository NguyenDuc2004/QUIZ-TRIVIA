package com.datn.quizai.realtime.service;

import com.datn.quizai.realtime.dto.GameEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Đẩy sự kiện phòng ra Redis Pub/Sub (FR-24).
 * <p>
 * <b>Vì sao không gửi thẳng vào broker?</b> Broker của Spring nằm trong bộ nhớ từng instance.
 * Nếu instance A gửi thẳng, người chơi đang giữ WebSocket ở instance B sẽ không nhận được gì.
 * Nên mọi sự kiện đều đi một đường duy nhất: publish lên kênh {@code room:{code}:events},
 * mọi instance cùng nghe rồi {@code GameEventRelay} chuyển tiếp xuống client của riêng mình.
 * <p>
 * Instance vừa publish cũng nhận lại chính message đó — đúng như mong muốn, vì đây là con đường
 * duy nhất tới broker cục bộ. Không được gửi song song cả hai kiểu, sẽ thành gửi trùng.
 */
@Service
public class GameEventPublisher {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public GameEventPublisher(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    /** Tên kênh Pub/Sub của một phòng — {@code GameEventRelay} nghe theo mẫu {@code room:*:events}. */
    public static String channel(String roomCode) {
        return "room:" + roomCode + ":events";
    }

    /** Phát cho toàn phòng. */
    public void broadcast(String roomCode, GameEvent event) {
        publish(new Envelope(roomCode, null, event));
    }

    /** Gửi riêng cho một người trong phòng (kết quả câu trả lời của chính họ). */
    public void toUser(String roomCode, UUID userId, GameEvent event) {
        publish(new Envelope(roomCode, userId, event));
    }

    private void publish(Envelope envelope) {
        try {
            redis.convertAndSend(channel(envelope.roomCode()), objectMapper.writeValueAsString(envelope));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Không tuần tự hoá được sự kiện phòng", e);
        }
    }

    /**
     * Gói tin trên kênh Redis.
     *
     * @param targetUserId null = phát cho cả phòng; có giá trị = chỉ gửi cho người này
     */
    public record Envelope(String roomCode, UUID targetUserId, GameEvent event) {
    }
}
