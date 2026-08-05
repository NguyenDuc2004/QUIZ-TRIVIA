package com.datn.quizai.quiz.domain;

/**
 * Phạm vi hiển thị quiz.
 * <p>
 * {@code PUBLIC}: Guest xem được phần giới thiệu (không kèm câu hỏi).
 * {@code PRIVATE}: chỉ chủ sở hữu và Admin thấy.
 */
public enum Visibility {
    PUBLIC, PRIVATE
}
