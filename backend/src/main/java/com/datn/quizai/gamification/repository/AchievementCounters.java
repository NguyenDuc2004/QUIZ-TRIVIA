package com.datn.quizai.gamification.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Đọc các con số dùng để xét điều kiện huy hiệu (features/13, FR-50).
 * <p>
 * Không lưu sẵn các con số này vào {@code user_stats} mà tính khi cần. Lý do: chúng đã có nguồn sự thật ở
 * bảng khác ({@code quiz_attempts}, {@code flashcard_reviews}), và giữ thêm một bản đếm là giữ thêm một chỗ
 * có thể lệch. Huy hiệu chỉ xét khi có hành động mới nên số lượt truy vấn nhỏ.
 */
@Repository
public class AchievementCounters {

    /**
     * Ngưỡng coi một thẻ là "đã thuộc" — trùng với ngưỡng ở
     * {@code FlashcardReviewRepository.countMastered}. Hai chỗ dùng chung một con số quy ước của SM-2.
     */
    private static final int NGUONG_DA_THUOC = 21;

    private final JdbcTemplate jdbc;

    public AchievementCounters(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Số bài làm đúng 100%.
     * <p>
     * {@code max_score > 0} để loại bài không có câu nào — bài rỗng thì {@code 0 = 0} và sẽ được tính là
     * hoàn hảo, tức trao huy hiệu "Điểm tuyệt đối" cho một bài không làm gì.
     */
    public long soBaiHoanHao(UUID userId) {
        Long n = jdbc.queryForObject("""
                select count(*) from quiz_attempts
                where user_id = ? and status = 'SUBMITTED'
                  and max_score > 0 and total_score = max_score
                """, Long.class, userId);
        return n == null ? 0 : n;
    }

    /** Số thẻ ghi nhớ đã thuộc — khoảng ôn từ 21 ngày trở lên. */
    public long soTheDaThuoc(UUID userId) {
        Long n = jdbc.queryForObject("""
                select count(*) from flashcard_reviews
                where user_id = ? and interval_days >= ?
                """, Long.class, userId, NGUONG_DA_THUOC);
        return n == null ? 0 : n;
    }
}
