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
    /**
     * Đã thử gọi AI nhưng không chấm được (hết hạn mức, mạng lỗi, JSON sai định dạng).
     * <p>
     * Cần một trạng thái <b>dừng</b> riêng: nếu để nguyên {@link #PENDING_AI} thì người học nhìn
     * thấy "đang chấm" vĩnh viễn và không ai biết là đã hỏng. Câu ở trạng thái này chờ Creator
     * chấm tay ({@link #HUMAN}).
     */
    AI_FAILED,
    /** Giáo viên chấm tay, ghi đè điểm AI. */
    HUMAN
}
