package com.datn.quizai.attempt.domain;

/** Vòng đời một lượt làm bài. */
public enum AttemptStatus {
    /** Đang làm, còn nhận được câu trả lời. */
    IN_PROGRESS,
    /** Người dùng bấm nộp và bài đã được chấm. */
    SUBMITTED,
    /** Hết giờ nên hệ thống tự nộp (FR-16) — vẫn được chấm trên phần đã trả lời. */
    EXPIRED;

    public boolean isFinished() {
        return this != IN_PROGRESS;
    }
}
