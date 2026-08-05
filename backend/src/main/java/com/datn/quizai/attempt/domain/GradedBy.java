package com.datn.quizai.attempt.domain;

/** Ai/cái gì đã chấm câu trả lời này. */
public enum GradedBy {
    /** Chưa chấm (bài đang làm dở, hoặc bỏ trống không trả lời). */
    NOT_GRADED,
    /** Máy tự chấm bằng cách so với đáp án cố định — không tốn chi phí AI. */
    AUTO,
    /** Câu tự luận đã nộp, đang chờ AI chấm (features/06). */
    PENDING_AI,
    /** AI đã chấm và có nhận xét. */
    AI,
    /** Giáo viên chấm tay, ghi đè điểm AI. */
    HUMAN
}
