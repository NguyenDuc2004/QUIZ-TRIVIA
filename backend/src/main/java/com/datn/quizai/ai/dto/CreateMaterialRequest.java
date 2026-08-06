package com.datn.quizai.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Nạp học liệu bằng cách dán thẳng văn bản (không tải file).
 * Tiện cho ghi chú ngắn và cho việc kiểm thử mà không cần chuẩn bị file.
 */
public record CreateMaterialRequest(
        @NotBlank(message = "Tiêu đề không được để trống")
        @Size(max = 300, message = "Tiêu đề tối đa 300 ký tự")
        String title,

        @Size(max = 100, message = "Chủ đề tối đa 100 ký tự")
        String topic,

        @NotBlank(message = "Nội dung không được để trống")
        @Size(min = 100, message = "Nội dung cần ít nhất 100 ký tự thì mới sinh đề được")
        String content
) {
}
