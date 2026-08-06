package com.datn.quizai.attempt.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Một dòng bảng xếp hạng của quiz (FR-19).
 * Mỗi người chỉ xuất hiện một lần với bài làm tốt nhất; đồng điểm thì ai nộp sớm hơn xếp trên.
 */
public record LeaderboardEntryResponse(
        int rank,
        UUID userId,
        String displayName,
        int totalScore,
        int maxScore,
        Integer durationSec,
        OffsetDateTime submittedAt
) {
}
