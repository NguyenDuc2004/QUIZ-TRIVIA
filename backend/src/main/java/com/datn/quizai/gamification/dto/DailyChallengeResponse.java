package com.datn.quizai.gamification.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Thử thách hôm nay kèm tiến độ của người đang gọi (features/13, FR-52).
 *
 * @param progress số hành động đã làm được
 * @param target   mục tiêu cần đạt
 * @param completedAt {@code null} = chưa hoàn thành
 */
public record DailyChallengeResponse(
        UUID id,
        LocalDate ngay,
        String description,
        int progress,
        int target,
        int xpReward,
        OffsetDateTime completedAt
) {
}
