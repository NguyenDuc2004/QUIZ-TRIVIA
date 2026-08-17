package com.datn.quizai.integrity.dto;

import com.datn.quizai.integrity.domain.ReviewStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Báo cáo tính toàn vẹn của một lượt thi (features/12, FR-47).
 *
 * @param riskScore 0–100. <b>Không phải xác suất gian lận</b> — là tổng có trọng số của các tín hiệu, dùng để
 *                  so sánh giữa các bài và để xếp thứ tự rà soát
 * @param flags     lý do cụ thể, thứ người rà soát thật sự đọc. Một con số 70 mà không nói vì sao thì không
 *                  dùng được vào việc gì
 * @param aiNote    {@code null} = chưa gọi mô hình hoặc gọi thất bại; khác chuỗi rỗng
 * @param canhBao   câu nhắc luôn kèm theo báo cáo: tín hiệu client giả mạo được, nên đây là cảnh báo hỗ trợ
 *                  quyết định, không phải bằng chứng
 */
public record IntegrityReport(
        UUID attemptId,
        String tenQuiz,
        String tenNguoiLam,
        int riskScore,
        boolean biGanCo,
        List<String> flags,
        String aiNote,
        ReviewStatus reviewStatus,
        OffsetDateTime reviewedAt,
        String reviewNote,
        long soSuKien,
        List<SuKien> suKien,
        String canhBao
) {
    public record SuKien(String type, String detail, OffsetDateTime occurredAt) {
    }
}
