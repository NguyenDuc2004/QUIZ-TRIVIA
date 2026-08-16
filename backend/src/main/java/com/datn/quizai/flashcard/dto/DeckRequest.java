package com.datn.quizai.flashcard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Tạo hoặc sửa một bộ thẻ (features/11, FR-37). */
public record DeckRequest(
        @NotBlank(message = "Tên bộ thẻ không được để trống")
        @Size(max = 200, message = "Tên bộ thẻ tối đa 200 ký tự")
        String title,

        @Size(max = 2000, message = "Mô tả tối đa 2000 ký tự")
        String description,

        @Size(max = 100, message = "Chủ đề tối đa 100 ký tự")
        String topic
) {
}
