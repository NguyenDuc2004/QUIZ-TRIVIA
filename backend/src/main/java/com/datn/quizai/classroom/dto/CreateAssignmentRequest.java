package com.datn.quizai.classroom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Giao một quiz cho lớp (features/14, FR-55).
 *
 * @param openAt null = mở ngay
 * @param dueAt  null = không có hạn. Không bắt buộc vì bắt buộc thì giáo viên phải bịa một ngày, và một hạn
 *               bịa ra còn tệ hơn không có hạn: học sinh vẫn thấy "quá hạn" đỏ lòm cho một bài không ai vội
 */
public record CreateAssignmentRequest(

        @NotNull(message = "Phải chọn quiz để giao")
        UUID quizId,

        @NotBlank(message = "Tiêu đề bài tập không được để trống")
        @Size(max = 200, message = "Tiêu đề tối đa 200 ký tự")
        String title,

        @Size(max = 2000, message = "Hướng dẫn tối đa 2000 ký tự")
        String instruction,

        OffsetDateTime openAt,
        OffsetDateTime dueAt
) {
}
