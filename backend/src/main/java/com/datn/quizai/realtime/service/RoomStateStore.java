package com.datn.quizai.realtime.service;

import com.datn.quizai.common.exception.BusinessException;
import com.datn.quizai.realtime.domain.RoomState;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;

/**
 * Đọc/ghi trạng thái phòng ở Redis key {@code room:{code}} (docs/database.md §3).
 * <p>
 * <b>Vì sao cần khoá:</b> nhiều người chơi bấm đáp án gần như cùng lúc. Nếu mỗi luồng đọc JSON,
 * cộng điểm rồi ghi đè thì hai lượt trả lời đồng thời sẽ mất một lượt (lost update). Nên mọi
 * thay đổi đi qua {@link #update} — đọc, sửa và ghi trong lúc đang giữ khoá
 * {@code room:{code}:lock}.
 * <p>
 * Khoá có TTL ngắn để tiến trình chết giữa chừng không treo phòng vĩnh viễn.
 */
@Service
public class RoomStateStore {

    private static final String KEY_PREFIX = "room:";
    private static final String LOCK_SUFFIX = ":lock";

    /** Phòng tự biến mất sau khoảng này nếu không ai động tới — không cần job dọn dẹp. */
    private static final Duration ROOM_TTL = Duration.ofHours(6);
    private static final Duration LOCK_TTL = Duration.ofSeconds(3);
    private static final int LOCK_MAX_ATTEMPTS = 100;
    private static final long LOCK_RETRY_MILLIS = 20;

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public RoomStateStore(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public Optional<RoomState> find(String roomCode) {
        String json = redis.opsForValue().get(key(roomCode));
        return Optional.ofNullable(json).map(this::deserialize);
    }

    public RoomState require(String roomCode) {
        return find(roomCode).orElseThrow(() ->
                BusinessException.notFound("Phòng không tồn tại hoặc đã kết thúc"));
    }

    public void save(RoomState state) {
        redis.opsForValue().set(key(state.roomCode()), serialize(state), ROOM_TTL);
    }

    /**
     * Đọc — biến đổi — ghi trong một khoá, để hai lượt trả lời đồng thời không đè điểm của nhau.
     *
     * @return trạng thái sau khi biến đổi
     */
    public RoomState update(String roomCode, UnaryOperator<RoomState> mutator) {
        String lockKey = key(roomCode) + LOCK_SUFFIX;
        String token = UUID.randomUUID().toString();

        acquire(lockKey, token);
        try {
            RoomState next = mutator.apply(require(roomCode));
            save(next);
            return next;
        } finally {
            release(lockKey, token);
        }
    }

    public void delete(String roomCode) {
        redis.delete(key(roomCode));
    }

    // ------------------------------------------------------------------ nội bộ

    private void acquire(String lockKey, String token) {
        for (int attempt = 0; attempt < LOCK_MAX_ATTEMPTS; attempt++) {
            if (Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(lockKey, token, LOCK_TTL))) {
                return;
            }
            try {
                Thread.sleep(LOCK_RETRY_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Bị ngắt khi chờ khoá phòng " + lockKey, e);
            }
        }
        throw new IllegalStateException("Không lấy được khoá phòng sau nhiều lần thử: " + lockKey);
    }

    /**
     * Chỉ xoá khoá nếu đúng là khoá mình đặt: khoá có thể đã hết hạn và được tiến trình khác
     * lấy mất, xoá bừa sẽ mở khoá cho phiên của người ta.
     */
    private void release(String lockKey, String token) {
        if (token.equals(redis.opsForValue().get(lockKey))) {
            redis.delete(lockKey);
        }
    }

    private String key(String roomCode) {
        return KEY_PREFIX + roomCode;
    }

    private String serialize(RoomState state) {
        try {
            return objectMapper.writeValueAsString(state);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Không tuần tự hoá được trạng thái phòng", e);
        }
    }

    private RoomState deserialize(String json) {
        try {
            return objectMapper.readValue(json, RoomState.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Không đọc được trạng thái phòng từ Redis", e);
        }
    }
}
