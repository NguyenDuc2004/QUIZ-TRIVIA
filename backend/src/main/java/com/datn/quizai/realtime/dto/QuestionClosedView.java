package com.datn.quizai.realtime.dto;

import java.util.List;
import java.util.UUID;

/**
 * Công bố đáp án khi câu hỏi đã đóng. Đây là lần đầu tiên đáp án đúng rời khỏi server
 * trong một ván đấu.
 */
public record QuestionClosedView(
        UUID questionId,
        List<UUID> correctOptionIds,
        String explanation,
        List<RoomPlayerView> leaderboard
) {
}
