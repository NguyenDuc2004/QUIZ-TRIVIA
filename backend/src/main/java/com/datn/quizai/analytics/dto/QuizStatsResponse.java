package com.datn.quizai.analytics.dto;

import java.util.List;
import java.util.UUID;

/**
 * Thống kê một quiz, dành cho chủ quiz (FR-27).
 *
 * @param completionPercent tỉ lệ lượt nộp kịp giờ; phần còn lại là lượt bị hết giờ. Con số này nói
 *                          lên đề có quá dài hay thời gian đặt quá ngắn
 * @param scoreDistribution mười khoảng 10% — đủ để vẽ biểu đồ cột, luôn trả đủ 10 phần tử kể cả
 *                          khoảng rỗng, để client không phải tự chèn số 0 vào chỗ trống
 * @param hardestQuestions  câu bị làm sai nhiều nhất, đã lọc bỏ câu quá ít lượt trả lời
 */
public record QuizStatsResponse(
        long totalAttempts,
        long distinctLearners,
        Double averagePercent,
        Double completionPercent,
        List<ScoreBucket> scoreDistribution,
        List<HardQuestion> hardestQuestions
) {
    /**
     * Một khoảng điểm.
     *
     * @param label nhãn sẵn dùng ("0–10%"), để nhãn trên trục biểu đồ và cách chia khoảng không
     *              bao giờ lệch nhau
     */
    public record ScoreBucket(int fromPercent, int toPercent, String label, long attemptCount) {
    }

    /**
     * Một câu hỏi khó.
     *
     * @param wrongPercent tỉ lệ trả lời sai — thứ dùng để sắp xếp; số lượt trả lời trả kèm để người
     *                     đọc tự đánh giá con số đó đáng tin tới đâu
     */
    public record HardQuestion(
            UUID questionId,
            String content,
            String topic,
            long answeredCount,
            long wrongCount,
            double wrongPercent
    ) {
    }
}
