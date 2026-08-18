package com.datn.quizai.integrity.dto;

import com.datn.quizai.integrity.domain.ReviewStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Kết luận của người rà soát (features/12, FR-47).
 *
 * @param status không nhận {@code PENDING}: đó là trạng thái ban đầu do hệ thống đặt, không phải một kết luận
 *               ai có thể chọn
 * @param note   ghi chú lý do. Không bắt buộc, nhưng một kết luận "không hợp lệ" mà không có lý do thì người
 *               bị kết luận không có gì để phản hồi
 */
public record ReviewRequest(
        @NotNull(message = "Phải chọn kết luận")
        ReviewStatus status,

        @Size(max = 1000, message = "Ghi chú tối đa 1000 ký tự")
        String note
) {
}
