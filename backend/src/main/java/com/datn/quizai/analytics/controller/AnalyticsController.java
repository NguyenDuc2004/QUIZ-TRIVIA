package com.datn.quizai.analytics.controller;

import com.datn.quizai.analytics.dto.LearnerProgressResponse;
import com.datn.quizai.analytics.dto.QuizAttemptSummary;
import com.datn.quizai.analytics.dto.QuizStatsResponse;
import com.datn.quizai.analytics.service.AnalyticsService;
import com.datn.quizai.auth.service.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Thống kê — docs/api.md §8.
 * <p>
 * Toàn bộ yêu cầu <b>đăng nhập</b>. Hai phạm vi khác nhau: {@code /me} là dữ liệu của chính người
 * gọi; {@code /quizzes/{id}} là dữ liệu tổng hợp của quiz mình sở hữu.
 */
@RestController
@RequestMapping("/api/v1/analytics")
@Tag(name = "Analytics", description = "Tiến độ học tập và thống kê quiz")
@SecurityRequirement(name = "bearerAuth")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/me")
    @Operation(summary = "Tiến độ học tập của tôi (FR-26): số lượt, số quiz, điểm trung bình và "
            + "đường điểm theo thời gian. Điểm mạnh/yếu theo chủ đề nằm ở /recommendations/path.")
    public LearnerProgressResponse myProgress(
            @AuthenticationPrincipal JwtService.AuthenticatedUser current) {
        return analyticsService.myProgress(current.id());
    }

    @GetMapping("/quizzes/{quizId}")
    @Operation(summary = "Thống kê quiz của tôi (FR-27): tỉ lệ nộp kịp giờ, phân bố điểm theo 10 "
            + "khoảng, và câu bị làm sai nhiều nhất. Quiz của người khác trả 404.")
    public QuizStatsResponse quizStats(
            @AuthenticationPrincipal JwtService.AuthenticatedUser current,
            @PathVariable UUID quizId) {
        return analyticsService.quizStats(quizId, current);
    }

    @GetMapping("/quizzes/{quizId}/attempts")
    @Operation(summary = "Bài làm trên quiz của tôi, kèm cờ cần chấm tay. Đây là cửa vào của việc "
            + "chấm tay câu tự luận — API ghi đè điểm nằm ở PATCH /attempts/{a}/answers/{b}/grade.")
    public List<QuizAttemptSummary> quizAttempts(
            @AuthenticationPrincipal JwtService.AuthenticatedUser current,
            @PathVariable UUID quizId) {
        return analyticsService.quizAttempts(quizId, current);
    }
}
