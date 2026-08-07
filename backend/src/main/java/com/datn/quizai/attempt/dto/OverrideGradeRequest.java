package com.datn.quizai.attempt.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Chủ quiz chấm tay, ghi đè điểm AI (docs/features/06 §Use case).
 *
 * @param score    điểm mới; trần trên là {@code max_score} của câu, kiểm ở tầng service vì giá trị
 *                 đó phụ thuộc từng câu chứ không phải hằng số khai báo được ở đây
 * @param feedback nhận xét thay cho nhận xét của AI; bỏ trống thì xoá nhận xét cũ
 */
public record OverrideGradeRequest(

        @NotNull(message = "Phải nhập điểm")
        @Min(value = 0, message = "Điểm không được âm")
        Integer score,

        @Size(max = 2000, message = "Nhận xét tối đa 2000 ký tự")
        String feedback
) {
}
