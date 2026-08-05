package com.datn.quizai.quiz.domain;

/** Năm loại câu hỏi hệ thống hỗ trợ (FR-9). */
public enum QuestionType {
    /** Trắc nghiệm một đáp án đúng. */
    SINGLE_CHOICE,
    /** Trắc nghiệm nhiều đáp án đúng. */
    MULTIPLE_CHOICE,
    /** Đúng/Sai. */
    TRUE_FALSE,
    /** Điền vào chỗ trống — mỗi lựa chọn là một đáp án được chấp nhận. */
    FILL_BLANK,
    /** Trả lời ngắn/tự luận — lưu một đáp án mẫu, do AI chấm (features/06). */
    SHORT_ANSWER;

    /** Loại câu hỏi mà người dùng chọn trong danh sách lựa chọn cho sẵn. */
    public boolean isChoiceBased() {
        return this == SINGLE_CHOICE || this == MULTIPLE_CHOICE || this == TRUE_FALSE;
    }
}
