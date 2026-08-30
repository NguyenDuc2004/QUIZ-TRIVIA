package com.datn.quizai.recommend.dto;

import java.util.List;
import java.util.UUID;

/**
 * Một quiz được gợi ý, kèm <b>lý do</b>.
 *
 * @param title        lấy từ <b>PostgreSQL</b>, không lấy từ nhãn trên nút đồ thị: nhãn trong Neo4j
 *                     là bản sao, và nó cũ đi mỗi khi chủ quiz đổi tên mà chưa có bài nộp mới để
 *                     kích hoạt đồng bộ
 * @param thumbnailUrl ảnh bìa; {@code null} khi quiz chưa có ảnh — frontend vẽ ô trống cùng kích
 *                     thước chứ không bịa ảnh thay thế
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
        String thumbnailUrl,
        RecommendationSource source,
        String reason,
        List<String> weakTopics,
        long peerCount,
        long attemptCount,
        String categoryName
) {
    /**
     * Thay tiêu đề và ảnh bìa bằng bản mới nhất đọc từ PostgreSQL.
     * <p>
     * Đồ thị chỉ nên giữ <b>quan hệ</b>; mọi thứ để hiển thị lấy từ nguồn sự thật. Nhân bản thêm
     * ảnh bìa sang Neo4j thì mỗi lần chủ quiz đổi ảnh, thẻ gợi ý lại trỏ vào file cũ đã bị xoá.
     */
    public RecommendedQuizResponse withDisplayData(String freshTitle, String freshThumbnailUrl,
                                                  String freshCategoryName) {
        return new RecommendedQuizResponse(
                quizId, freshTitle == null ? title : freshTitle, freshThumbnailUrl,
                source, reason, weakTopics, peerCount, attemptCount, freshCategoryName);
    }

    /** Vì sao quiz này lọt vào danh sách — để giao diện nhóm và để đánh giá chất lượng gợi ý. */
    public enum RecommendationSource {
        /** Quiz chạm vào chủ đề người học đang làm sai nhiều. */
        WEAK_TOPIC,
        /** Người có hành vi làm bài giống mình đã làm quiz này. */
        SIMILAR_LEARNERS,
        /**
         * Quiz thuộc chủ đề người học chưa từng luyện.
         * <p>
         * Nguồn dự phòng khi hai nguồn trên cạn — và là nguồn duy nhất có tác dụng với người vừa
         * đăng ký, vốn chưa có hành vi nào để phân tích.
         */
        NEW_TOPIC
    }
}
