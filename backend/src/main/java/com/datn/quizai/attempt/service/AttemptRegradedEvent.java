package com.datn.quizai.attempt.service;

import java.util.UUID;

/**
 * AI vừa chấm xong câu tự luận của một bài và tổng điểm đã được cộng lại.
 * <p>
 * Tách khỏi {@link AttemptSubmittedEvent} vì hai thời điểm khác nhau: lúc nộp, câu tự luận còn 0
 * điểm nên năng lực theo chủ đề tính ra <b>sai</b>. Đồ thị gợi ý phải được dựng lại sau khi có điểm
 * thật, nếu không người học sẽ bị đánh giá là yếu ở chủ đề mà họ vừa làm tốt.
 * <p>
 * Phát ra từ luồng nền, ngoài transaction — người nhận phải là {@code @EventListener} thường, không
 * phải {@code @TransactionalEventListener} (không có transaction nào để bám vào).
 */
public record AttemptRegradedEvent(UUID attemptId) {
}
