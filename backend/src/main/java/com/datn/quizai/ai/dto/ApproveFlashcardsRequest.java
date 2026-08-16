package com.datn.quizai.ai.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Người dùng duyệt các thẻ AI sinh ra (human-in-the-loop, features/11 FR-38).
 * <p>
 * Chọn từng thẻ chứ không phải "lưu tất": mô hình có thể trả về thẻ đúng định dạng nhưng sai nội dung, và
 * một thẻ sai lọt vào bộ sẽ được người học <i>ôn đi ôn lại</i> theo lịch SRS — tức được học thuộc, chứ
 * không chỉ được đọc qua một lần như một câu hỏi trong đề.
 *
 * @param indexes vị trí các thẻ được chọn trong mảng kết quả của job
 */
public record ApproveFlashcardsRequest(
        @NotEmpty(message = "Phải chọn ít nhất một thẻ để lưu")
        List<Integer> indexes
) {
}
