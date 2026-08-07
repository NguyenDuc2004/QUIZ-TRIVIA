package com.datn.quizai.attempt.service;

import java.util.UUID;

/**
 * Một bài làm vừa được nộp (hoặc hết giờ và bị chốt).
 * <p>
 * Phát cho <b>mọi</b> bài nộp; hai bên nghe với hai mục đích khác nhau: chấm câu tự luận
 * (features/06) và đồng bộ đồ thị gợi ý (features/07). Bên nào không có việc thì tự thoát sớm.
 * <p>
 * Chỉ mang {@code attemptId} chứ không mang cả đối tượng: người nhận chạy ở luồng khác, sau khi
 * transaction đã commit, nên phải tự nạp lại dữ liệu tươi chứ không dùng entity đã thoát khỏi
 * persistence context.
 */
public record AttemptSubmittedEvent(UUID attemptId, UUID userId) {
}
