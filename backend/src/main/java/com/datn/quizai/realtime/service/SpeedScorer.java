package com.datn.quizai.realtime.service;

/**
 * Tính điểm phòng đấu theo <b>độ chính xác + tốc độ</b> (FR-22).
 * <p>
 * Công thức, cho một câu có {@code points} điểm gốc:
 * <pre>
 *   sai hoặc quá hạn → 0
 *   đúng             → points × (BASE + BONUS × phần thời gian còn lại)
 * </pre>
 * Trả lời đúng ngay lập tức được {@code points × 1000}, trả lời đúng sát giờ chót vẫn được
 * {@code points × 500}. Nghĩa là <b>đúng luôn hơn sai</b>, nhanh chỉ là phần thưởng cộng thêm —
 * tránh việc bấm bừa thật nhanh lại lợi hơn suy nghĩ rồi trả lời đúng.
 * <p>
 * Lớp thuần logic, không phụ thuộc Spring nên test trực tiếp được.
 */
public final class SpeedScorer {

    /** Phần điểm nhận được chỉ nhờ trả lời đúng, không phụ thuộc tốc độ. */
    public static final int BASE = 500;
    /** Phần điểm thưởng tối đa cho tốc độ. */
    public static final int SPEED_BONUS = 500;

    private SpeedScorer() {
    }

    /**
     * @param points        điểm gốc của câu hỏi ({@code questions.points})
     * @param correct       đáp án có đúng không
     * @param elapsedMillis thời gian từ lúc server phát câu hỏi tới lúc nhận đáp án
     *                      (đo <b>ở server</b>, không lấy từ client)
     * @param limitMillis   thời gian tối đa cho câu này
     */
    public static int score(int points, boolean correct, long elapsedMillis, long limitMillis) {
        if (!correct || limitMillis <= 0 || elapsedMillis > limitMillis) {
            return 0;
        }

        long clamped = Math.max(0, elapsedMillis);
        double remainingRatio = 1.0 - (double) clamped / limitMillis;

        return (int) Math.round(points * (BASE + SPEED_BONUS * remainingRatio));
    }
}
