package com.datn.quizai.attempt.service;

import java.util.UUID;

/**
 * Một bài làm vừa được nộp và có câu tự luận chờ AI chấm (docs/features/06).
 * <p>
 * Chỉ mang {@code attemptId} chứ không mang cả đối tượng: người nhận chạy ở luồng khác, sau khi
 * transaction đã commit, nên phải tự nạp lại dữ liệu tươi chứ không dùng entity đã thoát khỏi
 * persistence context.
 */
public record AttemptSubmittedEvent(UUID attemptId, UUID userId) {
}
