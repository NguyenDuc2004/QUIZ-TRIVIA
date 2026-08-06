package com.datn.quizai.realtime.dto;

import java.time.Instant;

/**
 * Gói chung mọi thông điệp server đẩy xuống phòng (docs/api.md §5.2).
 * <p>
 * Client phân nhánh theo {@code type} rồi ép {@code data} về đúng kiểu. Dùng một lớp bao thay vì
 * nhiều kênh riêng để client chỉ cần subscribe đúng một chỗ ({@code /topic/room/{code}}) và
 * không bao giờ nhận sự kiện lệch thứ tự giữa các kênh.
 *
 * @param at mốc thời gian của server — client dùng để chỉnh đồng hồ đếm ngược cho khớp
 */
public record GameEvent(GameEventType type, Instant at, Object data) {

    public static GameEvent of(GameEventType type, Object data) {
        return new GameEvent(type, Instant.now(), data);
    }
}
