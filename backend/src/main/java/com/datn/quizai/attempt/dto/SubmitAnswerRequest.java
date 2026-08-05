package com.datn.quizai.attempt.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Trả lời một câu. Tùy loại câu hỏi mà client gửi {@code optionIds} (trắc nghiệm)
 * hoặc {@code text} (điền khuyết / tự luận); gửi cả hai đều rỗng nghĩa là bỏ trống câu đó.
 */
public record SubmitAnswerRequest(
        @NotNull(message = "Thiếu questionId") UUID questionId,
        List<UUID> optionIds,
        String text
) {
}
