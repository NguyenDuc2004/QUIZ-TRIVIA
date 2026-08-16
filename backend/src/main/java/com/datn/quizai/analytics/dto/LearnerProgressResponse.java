package com.datn.quizai.analytics.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Tiến độ học tập của chính người gọi (FR-85).
 * <p>
 * <b>Không có phần "điểm mạnh/yếu theo chủ đề" ở đây.</b> Nó đã nằm ở trang Lộ trình học
 * (features/07), tính từ đồ thị Neo4j. Làm lại phép tính đó từ PostgreSQL sẽ có hai màn hình nói về
 * cùng một chuyện, tính bằng hai cách, trên hai kho dữ liệu — và chúng sẽ lệch nhau vào một ngày
 * nào đó. Trang tiến độ trỏ người dùng sang đó thay vì tự trả lời.
 *
 * @param averagePercent điểm trung bình theo phần trăm; null khi chưa làm bài nào — <b>không</b>
 *                       trả 0, vì 0% nghĩa là làm mà sai hết, khác hẳn với chưa làm gì
 */
public record LearnerProgressResponse(
        long totalAttempts,
        long distinctQuizzes,
        Double averagePercent,
        List<AttemptScore> trend
) {
    /** Một lượt làm bài trên đường tiến bộ. */
    public record AttemptScore(
            OffsetDateTime submittedAt,
            String quizTitle,
            int score,
            int maxScore,
            double percent
    ) {
    }
}
