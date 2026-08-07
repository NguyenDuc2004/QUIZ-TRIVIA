package com.datn.quizai.ai.grading;

/**
 * Kết quả chấm một câu tự luận (docs/features/06 §Đầu ra AI).
 *
 * @param score       điểm đã được ép về khoảng [0, maxScore] — không tin thẳng số mô hình trả
 * @param correct     coi là đúng hay không; suy ra từ điểm khi mô hình không nói rõ
 * @param feedback    nhận xét bài đã làm, hiện cho người học
 * @param suggestions việc cần làm để khá hơn; rỗng khi đã đạt điểm tối đa
 */
public record AiGrade(int score, boolean correct, String feedback, String suggestions) {
}
