package com.datn.quizai.attempt.dto;

/**
 * Giải thích đáp án do AI viết (docs/features/06).
 *
 * @param explanation nội dung giải thích; rỗng nếu mô hình không trả được gì dùng được
 */
public record ExplanationResponse(String explanation) {
}
