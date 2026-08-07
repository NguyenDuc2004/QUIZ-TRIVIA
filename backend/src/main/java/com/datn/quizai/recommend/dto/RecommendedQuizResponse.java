package com.datn.quizai.recommend.dto;

import java.util.List;
import java.util.UUID;

/**
 * Một quiz được gợi ý, kèm <b>lý do</b>.
 *
 * @param reason      vì sao quiz này được gợi ý — hiện thẳng lên thẻ. Gợi ý không nói lý do thì
 *                    người dùng không có căn cứ để tin hay bỏ qua, và cũng không phản hồi được
 *                    khi gợi ý sai
 * @param weakTopics  chủ đề đang yếu mà quiz này chạm tới; rỗng với gợi ý kiểu cộng tác
 * @param peerCount   số người có hành vi giống mình đã làm quiz này; 0 với gợi ý theo chủ đề yếu
 * @param attemptCount số lượt làm thật của quiz — <b>không phải</b> rating bịa ra
 */
public record RecommendedQuizResponse(
        UUID quizId,
        String title,
        RecommendationSource source,
        String reason,
        List<String> weakTopics,
        long peerCount,
        long attemptCount
) {
    /** Vì sao quiz này lọt vào danh sách — để giao diện nhóm và để đánh giá chất lượng gợi ý. */
    public enum RecommendationSource {
        /** Quiz chạm vào chủ đề người học đang làm sai nhiều. */
        WEAK_TOPIC,
        /** Người có hành vi làm bài giống mình đã làm quiz này. */
        SIMILAR_LEARNERS
    }
}
