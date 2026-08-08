package com.datn.quizai.recommend.dto;

import java.util.List;

/**
 * Danh sách gợi ý <b>kèm lý do khi rỗng</b> (FR-34).
 * <p>
 * Trước đây API trả thẳng một mảng. Mảng rỗng thì giao diện ẩn cả khu Gợi ý, và người dùng không có
 * cách nào phân biệt <i>"hệ thống không còn gì để gợi ý"</i> với <i>"tính năng hỏng"</i> — thực tế đã
 * hiểu nhầm thành hỏng. Cùng vấn đề, cùng cách giải với {@link LearningPathResponse#note()}.
 * <p>
 * Ba tình huống rỗng khác nhau hẳn về việc người dùng nên làm gì tiếp, nên không gộp thành một câu
 * chung chung: kho chưa có quiz nào, đã làm hết quiz đang có, và không truy vấn được đồ thị.
 *
 * @param note {@code null} khi danh sách <b>có</b> gợi ý — lúc đó không có gì cần giải thích, và
 *             hiện thêm một dòng chữ chỉ làm loãng
 */
public record RecommendationsResponse(
        List<RecommendedQuizResponse> items,
        String note
) {
    public static RecommendationsResponse of(List<RecommendedQuizResponse> items, String note) {
        return new RecommendationsResponse(items, items.isEmpty() ? note : null);
    }
}
