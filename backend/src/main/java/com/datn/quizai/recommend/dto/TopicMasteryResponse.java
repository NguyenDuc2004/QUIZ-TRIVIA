package com.datn.quizai.recommend.dto;

/**
 * Năng lực của người học trên một chủ đề — một chặng trong lộ trình (FR-35).
 *
 * @param accuracy         tỷ lệ đúng, 0..1
 * @param weak             có bị coi là yếu không; ngưỡng do tầng truy vấn quyết định, trả kèm để
 *                         giao diện không phải tự đoán lại và nói khác backend
 * @param availableQuizzes số quiz công khai thuộc chủ đề này mà người học chưa làm — chặng có 0
 *                         quiz thì lời khuyên "học tiếp đi" là lời khuyên suông
 */
public record TopicMasteryResponse(
        String topic,
        long correct,
        long total,
        double accuracy,
        boolean weak,
        long availableQuizzes
) {
}
