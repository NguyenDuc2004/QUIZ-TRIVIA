package com.datn.quizai.gamification.domain;

/**
 * Hành động sinh ra XP (features/13, FR-49).
 * <p>
 * Mỗi loại có cách dựng khoá tự nhiên riêng, và khoá đó là thứ chặn cộng trùng ở
 * {@code uk_xp_events (user_id, source_type, source_key)}.
 */
public enum XpSource {
    /** Nộp một bài quiz. Khoá = {@code attemptId} — một bài chỉ nộp một lần. */
    ATTEMPT_SUBMITTED,
    /**
     * Ôn một thẻ ghi nhớ. Khoá = {@code cardId:ngày}.
     * <p>
     * Có ngày trong khoá vì API ôn không chặn ôn sớm: không giới hạn thì bấm một thẻ trăm lần là trăm lần
     * XP. Ghép ngày vào biến nó thành "mỗi thẻ mỗi ngày một lần cộng", vẫn thưởng người ôn đều mà không
     * thưởng người bấm liên tục.
     */
    FLASHCARD_REVIEW,
    /** Hoàn thành một ván phòng đấu. Khoá = {@code roomId}. */
    ROOM_FINISHED,
    /** Hoàn thành thử thách ngày. Khoá = {@code challengeId}. */
    DAILY_CHALLENGE
}
