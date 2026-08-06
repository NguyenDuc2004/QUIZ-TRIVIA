package com.datn.quizai.quiz.dto;

import com.datn.quizai.quiz.domain.Difficulty;
import com.datn.quizai.quiz.domain.Visibility;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Dữ liệu tạo/cập nhật quiz (FR-7). */
public record QuizRequest(

        @NotBlank(message = "Tiêu đề không được để trống")
        @Size(max = 200, message = "Tiêu đề tối đa 200 ký tự")
        String title,

        @Size(max = 2000, message = "Mô tả tối đa 2000 ký tự")
        String description,

        UUID categoryId,

        /** Bỏ trống → MEDIUM. */
        Difficulty difficulty,

        /** Bỏ trống → PRIVATE (an toàn mặc định, Creator chủ động xuất bản). */
        Visibility visibility,

        @Min(value = 1, message = "Thời gian làm bài phải lớn hơn 0 giây")
        Integer timeLimitSec,

        /**
         * Đường dẫn ảnh bìa lấy từ {@code POST /api/v1/files/images}; bỏ trống để dùng khối màu tự sinh.
         * Chỉ nhận đường dẫn nội bộ {@code /uploads/…} — xem {@code QuizService.applyRequest}.
         */
        @Size(max = 500, message = "Đường dẫn ảnh tối đa 500 ký tự")
        String thumbnailUrl
) {
}
