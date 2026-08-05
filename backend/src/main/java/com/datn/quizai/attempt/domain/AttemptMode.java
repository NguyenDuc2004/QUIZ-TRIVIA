package com.datn.quizai.attempt.domain;

/** Hai chế độ làm bài (FR-14). */
public enum AttemptMode {
    /** Luyện tập: trả lời câu nào chấm ngay câu đó, hiện luôn giải thích. */
    PRACTICE,
    /** Thi: chỉ chấm khi nộp bài, trong lúc làm không lộ đáp án. */
    EXAM
}
