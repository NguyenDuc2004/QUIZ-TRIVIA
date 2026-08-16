package com.datn.quizai.flashcard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Tạo hoặc sửa một thẻ (features/11, FR-37). */
public record FlashcardRequest(
        @NotBlank(message = "Mặt trước không được để trống")
        @Size(max = 2000, message = "Mặt trước tối đa 2000 ký tự")
        String front,

        @NotBlank(message = "Mặt sau không được để trống")
        @Size(max = 4000, message = "Mặt sau tối đa 4000 ký tự")
        String back,

        @Size(max = 500, message = "Gợi ý tối đa 500 ký tự")
        String hint
) {
}
