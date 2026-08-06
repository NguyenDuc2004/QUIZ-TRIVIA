package com.datn.quizai.ai.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Creator duyệt các câu hỏi AI sinh ra (human-in-the-loop).
 *
 * @param indexes vị trí các câu được chọn trong mảng kết quả của job
 */
public record ApproveQuestionsRequest(
        @NotEmpty(message = "Phải chọn ít nhất một câu hỏi để lưu")
        List<Integer> indexes
) {
}
