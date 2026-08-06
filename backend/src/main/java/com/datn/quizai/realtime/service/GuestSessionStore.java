package com.datn.quizai.realtime.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 * Phiên của khách vãng lai trong phòng đấu, lưu ở Redis {@code roomguest:{key}}.
 * <p>
 * <b>Vì sao không cấp JWT cho khách:</b> JWT của hệ thống gắn với một tài khoản có thật và mang
 * vai trò RBAC; khách thì không có gì trong bảng {@code users}. Cấp JWT cho họ sẽ phải thêm vai
 * trò GUEST vào enum {@code Role}, kéo theo ràng buộc CHECK của bảng users và mọi chỗ phân quyền
 * — đổi rất nhiều thứ chỉ để phục vụ một phòng chơi.
 * <p>
 * Thay vào đó khách nhận một <b>khoá ngẫu nhiên gắn chặt với đúng một phòng</b>, tự hết hạn theo
 * TTL. Khoá này không mở được bất cứ API nào khác ngoài phòng đó.
 */
@Service
public class GuestSessionStore {

    private static final String KEY_PREFIX = "roomguest:";
    /** Đủ dài cho một ván đấu; phòng cũng chỉ sống 6 tiếng ở Redis. */
    private static final Duration TTL = Duration.ofHours(6);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public GuestSessionStore(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    /**
     * Danh tính khách.
     *
     * @param playerId id dùng trong phòng, cùng kiểu với id tài khoản để phần tính điểm không
     *                 phải phân biệt khách hay thành viên
     */
    public record GuestSession(UUID playerId, String roomCode, String displayName) {
    }

    /** @return khoá bí mật client giữ lại để nối WebSocket và gọi API của phòng */
    public String issue(UUID playerId, String roomCode, String displayName) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String key = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        redis.opsForValue().set(KEY_PREFIX + key,
                serialize(new GuestSession(playerId, roomCode, displayName)), TTL);
        return key;
    }

    public Optional<GuestSession> resolve(String guestKey) {
        if (guestKey == null || guestKey.isBlank()) {
            return Optional.empty();
        }
        String json = redis.opsForValue().get(KEY_PREFIX + guestKey.trim());
        return Optional.ofNullable(json).map(this::deserialize);
    }

    public void revoke(String guestKey) {
        if (guestKey != null && !guestKey.isBlank()) {
            redis.delete(KEY_PREFIX + guestKey.trim());
        }
    }

    private String serialize(GuestSession session) {
        try {
            return objectMapper.writeValueAsString(session);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Không tuần tự hoá được phiên khách", e);
        }
    }

    private GuestSession deserialize(String json) {
        try {
            return objectMapper.readValue(json, GuestSession.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Không đọc được phiên khách từ Redis", e);
        }
    }
}
