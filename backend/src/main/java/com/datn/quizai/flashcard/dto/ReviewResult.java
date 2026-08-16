package com.datn.quizai.flashcard.dto;

import java.time.LocalDate;

/**
 * Kết quả sau khi ôn một thẻ (features/11, FR-41).
 * <p>
 * Trả lại lịch mới để giao diện nói được <i>"gặp lại sau N ngày"</i> ngay tại chỗ. Không có phản hồi này
 * thì người học bấm một trong bốn nút mà không thấy hệ quả, và bốn nút trở thành như nhau.
 *
 * @param soTheConLai số thẻ còn đến hạn sau lần ôn này — dùng cho thanh tiến độ của phiên ôn
 */
public record ReviewResult(
        LocalDate dueDate,
        int intervalDays,
        int repetitions,
        long soTheConLai
) {
}
