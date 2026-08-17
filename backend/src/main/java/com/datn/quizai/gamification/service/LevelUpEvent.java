package com.datn.quizai.gamification.service;

import java.util.UUID;

/**
 * Người dùng vừa lên cấp (features/13, FR-53).
 * <p>
 * Là sự kiện miền chứ không phải lời gọi thẳng sang tính năng thông báo, cùng lý do như chính
 * gamification là listener của {@code AttemptSubmittedEvent}: {@code GamificationService} không cần biết
 * tính năng 16 tồn tại, và có thể bỏ hẳn tính năng 16 mà không sửa một dòng nào ở đây.
 *
 * @param capCu cấp trước khi cộng XP — giữ lại để thông báo nói được "cấp 3 → cấp 4" thay vì chỉ "cấp 4"
 */
public record LevelUpEvent(UUID userId, int capCu, int capMoi) {
}
