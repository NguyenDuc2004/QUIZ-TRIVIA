package com.datn.quizai.quiz.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Một lựa chọn/đáp án khi tạo câu hỏi.
 * Với FILL_BLANK mỗi phần tử là một đáp án được chấp nhận; với SHORT_ANSWER là đáp án mẫu.
 */
public record QuestionOptionRequest(

        @NotBlank(message = "Nội dung lựa chọn không được để trống")
        String content,

        boolean correct
) {
}
