package com.datn.quizai.realtime.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Tạo phòng đấu từ một quiz.
 *
 * @param secondsPerQuestion thời gian mỗi câu; bỏ trống thì lấy `questions.time_limit_sec` của
 *                           từng câu, không có nữa thì dùng mặc định của hệ thống
 */
public record CreateRoomRequest(
        @NotNull(message = "Thiếu quizId") UUID quizId,

        @Min(value = 5, message = "Mỗi câu tối thiểu 5 giây")
        Integer secondsPerQuestion
) {
}
