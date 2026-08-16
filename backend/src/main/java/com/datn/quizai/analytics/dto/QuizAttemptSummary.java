package com.datn.quizai.analytics.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Một bài làm trên quiz của tôi — dùng cho danh sách chấm tay (FR-86 + nợ từ features/06).
 * <p>
 * <b>Chỉ mang thông tin tổng hợp, không mang nội dung trả lời.</b> Danh sách này để Creator tìm bài
 * cần chấm; muốn xem bài thì mở chi tiết, và chỗ đó đã có kiểm tra quyền riêng. Đưa cả bài làm vào
 * danh sách là mở rộng phạm vi truy cập dữ liệu riêng tư mà không có lý do.
 *
 * @param needsManualGrading còn câu nào cần người chấm hay không — cờ này là lý do danh sách tồn tại
 */
public record QuizAttemptSummary(
        UUID attemptId,
        String learnerName,
        int score,
        int maxScore,
        OffsetDateTime submittedAt,
        long pendingAiCount,
        long failedAiCount,
        boolean needsManualGrading
) {
}
