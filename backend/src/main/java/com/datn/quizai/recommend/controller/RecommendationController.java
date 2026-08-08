package com.datn.quizai.recommend.controller;

import com.datn.quizai.auth.service.JwtService;
import com.datn.quizai.recommend.dto.LearningPathResponse;
import com.datn.quizai.recommend.dto.RecommendationsResponse;
import com.datn.quizai.recommend.service.GraphSyncService;
import com.datn.quizai.recommend.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Gợi ý cá nhân hoá từ đồ thị — docs/api.md §7.
 * <p>
 * <b>Yêu cầu đăng nhập.</b> Gợi ý dựa trên lịch sử làm bài của chính người gọi; không có khái niệm
 * "gợi ý cho khách" vì khách không có lịch sử. Khách xem danh sách quiz công khai như bình thường.
 */
@RestController
@RequestMapping("/api/v1/recommendations")
@Tag(name = "Recommendation", description = "Gợi ý quiz và lộ trình học dựa trên đồ thị Neo4j")
@SecurityRequirement(name = "bearerAuth")
public class RecommendationController {

    /** Đủ lấp một hàng thẻ trên trang chủ mà không bắt người dùng cuộn mãi. */
    private static final int DEFAULT_LIMIT = 8;
    private static final int MAX_LIMIT = 20;

    private final RecommendationService recommendationService;
    private final GraphSyncService graphSyncService;

    public RecommendationController(RecommendationService recommendationService,
                                    GraphSyncService graphSyncService) {
        this.recommendationService = recommendationService;
        this.graphSyncService = graphSyncService;
    }

    @GetMapping
    @Operation(summary = "Quiz gợi ý cho tôi (FR-34). Trộn ba nguồn: quiz chạm chủ đề tôi đang yếu, "
            + "quiz mà những người học có hành vi giống tôi đã làm, và chủ đề tôi chưa thử. Mỗi mục "
            + "kèm lý do; danh sách rỗng thì `note` nói rõ VÌ SAO rỗng.")
    public RecommendationsResponse recommend(
            @AuthenticationPrincipal JwtService.AuthenticatedUser current,
            @RequestParam(defaultValue = "" + DEFAULT_LIMIT) int limit) {
        return recommendationService.recommendQuizzes(current.id(), Math.clamp(limit, 1, MAX_LIMIT));
    }

    @org.springframework.web.bind.annotation.PostMapping("/rebuild")
    @Operation(summary = "Dựng lại đồ thị gợi ý của TÔI từ toàn bộ lịch sử làm bài. "
            + "Cần cho những bài đã làm trước khi có tính năng này, và để phục hồi nếu Neo4j mất dữ liệu.")
    public java.util.Map<String, Object> rebuild(
            @AuthenticationPrincipal JwtService.AuthenticatedUser current) {
        // Chỉ dựng lại phần của chính người gọi: không cần quyền quản trị, và không ai đụng được
        // vào dữ liệu của người khác.
        return java.util.Map.of("syncedAttempts", graphSyncService.rebuildForUser(current.id()));
    }

    @GetMapping("/path")
    @Operation(summary = "Lộ trình học đề xuất (FR-35): các chủ đề đã học xếp theo mức độ yếu đo "
            + "được, yếu nhất trước. Chưa đủ dữ liệu thì trả kèm ghi chú giải thích.")
    public LearningPathResponse learningPath(
            @AuthenticationPrincipal JwtService.AuthenticatedUser current) {
        return recommendationService.learningPath(current.id());
    }
}
