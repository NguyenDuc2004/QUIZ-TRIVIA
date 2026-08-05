package com.datn.quizai.quiz.dto;

import com.datn.quizai.quiz.domain.Difficulty;
import com.datn.quizai.quiz.domain.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Dữ liệu tạo/cập nhật câu hỏi (FR-9, FR-10).
 * Ràng buộc riêng theo từng loại câu hỏi được kiểm ở
 * {@link com.datn.quizai.quiz.service.QuestionService} (số lựa chọn, số đáp án đúng).
 */
public record QuestionRequest(

        @NotNull(message = "Phải chọn loại câu hỏi")
        QuestionType type,

        @NotBlank(message = "Nội dung câu hỏi không được để trống")
        String content,

        String explanation,

        Difficulty difficulty,

        @Size(max = 100, message = "Chủ đề tối đa 100 ký tự")
        String topic,

        @Min(value = 1, message = "Điểm phải lớn hơn 0")
        Integer points,

        @Min(value = 1, message = "Thời gian cho câu hỏi phải lớn hơn 0 giây")
        Integer timeLimitSec,

        @NotEmpty(message = "Phải có ít nhất một lựa chọn/đáp án")
        @Valid
        List<QuestionOptionRequest> options
) {
}
