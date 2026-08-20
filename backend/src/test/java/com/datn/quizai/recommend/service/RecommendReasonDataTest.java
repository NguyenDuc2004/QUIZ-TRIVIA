package com.datn.quizai.recommend.service;

import com.datn.quizai.recommend.dto.RecommendedQuizResponse;
import com.datn.quizai.recommend.dto.RecommendedQuizResponse.RecommendationSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phần dữ kiện đưa vào prompt giải thích gợi ý (features/07, FR-36).
 *
 * <h3>Đây là ranh giới chống ảo giác, không phải chuyện định dạng chuỗi</h3>
 * Mô hình chỉ nói được về những gì nó <b>được cho biết</b>. Nên chỗ duy nhất quyết định "mô hình có bịa
 * được hay không" chính là hàm dựng dữ kiện này — mọi ràng buộc trong system prompt đều chỉ là lớp thứ hai.
 * <p>
 * features/07 đã <b>bỏ trường {@code rating}</b> khỏi mô hình đồ thị vì hệ thống không có nguồn dữ liệu
 * đánh giá nào; nếu lời giải thích của AI lại nói *"quiz này được đánh giá cao"* thì đúng thứ vừa bị loại
 * bỏ vì trung thực lại quay về qua cửa sau.
 */
class RecommendReasonDataTest {

    @Test
    @DisplayName("Chỉ đưa vào dữ kiện CÓ THẬT: chủ đề yếu, số người tương tự, số lượt làm")
    void shouldOnlyIncludeRealFacts() {
        String duLieu = RecommendReasonService.duLieu(quiz(
                RecommendationSource.WEAK_TOPIC, List.of("Đạo hàm", "Tích phân"), 0, 42));

        assertThat(duLieu).contains("Đạo hàm").contains("Tích phân").contains("42");
    }

    @Test
    @DisplayName("Số 0 KHÔNG được đưa vào — nói 'có 0 người tương tự' là một dữ kiện vô nghĩa")
    void shouldOmitZeroCounts() {
        // Đưa vào thì mô hình sẽ cố diễn đạt nó thành câu, và câu đó chỉ có thể là một câu vô duyên
        // ("chưa có ai học giống bạn làm quiz này") — đúng thứ làm người học mất hứng chứ không giúp gì.
        String duLieu = RecommendReasonService.duLieu(quiz(
                RecommendationSource.NEW_TOPIC, List.of(), 0, 0));

        assertThat(duLieu).doesNotContain("0");
    }

    @Test
    @DisplayName("Chủ đề yếu rỗng thì không nhắc tới — không bịa ra một chủ đề nào")
    void shouldOmitEmptyWeakTopics() {
        String duLieu = RecommendReasonService.duLieu(quiz(
                RecommendationSource.SIMILAR_LEARNERS, List.of(), 7, 15));

        assertThat(duLieu).doesNotContain("đang làm sai nhiều");
        assertThat(duLieu).contains("7");
    }

    @Test
    @DisplayName("KHÔNG có đánh giá, số sao, hay bất cứ thứ gì hệ thống không đo được")
    void shouldNeverMentionRating() {
        // features/07 đã bỏ `rating` khỏi mô hình đồ thị VÌ hệ thống không có nguồn dữ liệu đó. Nếu dữ kiện
        // ở đây lỡ mang nó vào thì thứ vừa bị loại bỏ vì trung thực lại quay về qua cửa sau.
        String duLieu = RecommendReasonService.duLieu(quiz(
                RecommendationSource.WEAK_TOPIC, List.of("Mảng"), 3, 100));

        assertThat(duLieu.toLowerCase())
                .doesNotContain("sao").doesNotContain("rating").doesNotContain("đánh giá")
                .doesNotContain("chất lượng").doesNotContain("nổi tiếng");
    }

    @Test
    @DisplayName("Mỗi nguồn gợi ý được diễn đạt đúng bản chất của nó")
    void shouldDescribeEachSource() {
        assertThat(RecommendReasonService.duLieu(quiz(RecommendationSource.WEAK_TOPIC, List.of("X"), 0, 1)))
                .contains("đang yếu");
        assertThat(RecommendReasonService.duLieu(quiz(RecommendationSource.SIMILAR_LEARNERS, List.of(), 5, 1)))
                .contains("giống người này");
        assertThat(RecommendReasonService.duLieu(quiz(RecommendationSource.NEW_TOPIC, List.of(), 0, 1)))
                .contains("chưa từng luyện");
    }

    @Test
    @DisplayName("Tên quiz luôn có mặt — không có nó thì lời giải thích nói về một thứ vô danh")
    void shouldAlwaysIncludeTitle() {
        assertThat(RecommendReasonService.duLieu(quiz(RecommendationSource.NEW_TOPIC, List.of(), 0, 0)))
                .contains("Ôn tập Giải tích 1");
    }

    private static RecommendedQuizResponse quiz(RecommendationSource source, List<String> weakTopics,
                                                long peers, long attempts) {
        return new RecommendedQuizResponse(
                UUID.randomUUID(), "Ôn tập Giải tích 1", null, source,
                "lý do mẫu", weakTopics, peers, attempts);
    }
}
