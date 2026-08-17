package com.datn.quizai.gamification.service;

import java.util.UUID;

/**
 * Người dùng vừa mở khoá một huy hiệu (features/13, FR-53).
 * <p>
 * Mang theo {@code code} và {@code name} thay vì chỉ id: người nhận sự kiện dựng được nội dung thông báo mà
 * không phải đọc lại bảng {@code badges}, và {@code code} là thứ ổn định để giao diện chọn icon.
 */
public record BadgeEarnedEvent(UUID userId, String code, String name) {
}
