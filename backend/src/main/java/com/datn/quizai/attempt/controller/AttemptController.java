package com.datn.quizai.attempt.controller;

import com.datn.quizai.attempt.dto.AnswerFeedbackResponse;
import com.datn.quizai.attempt.dto.AttemptDetailResponse;
import com.datn.quizai.attempt.dto.AttemptSummaryResponse;
import com.datn.quizai.attempt.dto.ExplanationResponse;
import com.datn.quizai.attempt.dto.GradingViewResponse;
import com.datn.quizai.attempt.dto.LeaderboardEntryResponse;
import com.datn.quizai.attempt.dto.OverrideGradeRequest;
import com.datn.quizai.attempt.dto.StartAttemptRequest;
import com.datn.quizai.attempt.dto.SubmitAnswerRequest;
import com.datn.quizai.attempt.service.AttemptService;
import com.datn.quizai.auth.service.JwtService;
import com.datn.quizai.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Làm bài quiz đơn — docs/api.md §4.
 * <p>
 * <b>Mọi endpoint ở đây đều yêu cầu đăng nhập.</b> Guest chỉ được xem danh sách và trang giới
 * thiệu quiz công khai, không được làm bài (docs/features/01-auth.md §Quy tắc truy cập cho Guest);
 * quy tắc đó do {@code SecurityConfig} thực thi vì các đường dẫn dưới đây không nằm trong
 * danh sách công khai.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Attempt", description = "Làm bài quiz, chấm điểm và xem kết quả")
@SecurityRequirement(name = "bearerAuth")
public class AttemptController {

    private final AttemptService attemptService;

    public AttemptController(AttemptService attemptService) {
        this.attemptService = attemptService;
    }

    @PostMapping("/quizzes/{quizId}/attempts")
    @Operation(summary = "Bắt đầu làm bài; nếu đang có bài dở trên quiz này thì trả lại bài đó để làm tiếp")
    public ResponseEntity<AttemptDetailResponse> start(
            @AuthenticationPrincipal JwtService.AuthenticatedUser current,
            @PathVariable UUID quizId,
            @RequestBody(required = false) StartAttemptRequest request) {

        AttemptDetailResponse attempt = attemptService.start(
                quizId, request == null ? new StartAttemptRequest(null) : request, current);
        return ResponseEntity.status(HttpStatus.CREATED).body(attempt);
    }

    @GetMapping("/attempts/{attemptId}")
    @Operation(summary = "Xem bài làm: chưa nộp thì giấu đáp án, đã nộp thì kèm đáp án đúng và giải thích")
    public AttemptDetailResponse getDetail(@AuthenticationPrincipal JwtService.AuthenticatedUser current,
                                           @PathVariable UUID attemptId) {
        return attemptService.getDetail(attemptId, current);
    }

    @PostMapping("/attempts/{attemptId}/answers")
    @Operation(summary = "Trả lời một câu; chế độ luyện tập chấm và trả đáp án ngay")
    public AnswerFeedbackResponse answer(@AuthenticationPrincipal JwtService.AuthenticatedUser current,
                                         @PathVariable UUID attemptId,
                                         @Valid @RequestBody SubmitAnswerRequest request) {
        return attemptService.answer(attemptId, request, current);
    }

    @PostMapping("/attempts/{attemptId}/submit")
    @Operation(summary = "Nộp bài và chấm; gọi lại trên bài đã nộp thì trả đúng kết quả cũ")
    public AttemptDetailResponse submit(@AuthenticationPrincipal JwtService.AuthenticatedUser current,
                                        @PathVariable UUID attemptId) {
        return attemptService.submit(attemptId, current);
    }

    @PostMapping("/attempts/{attemptId}/answers/{answerId}/explain")
    @Operation(summary = "Nhờ AI giải thích một câu trong bài đã nộp (FR-30). "
            + "Chỉ chủ bài làm gọi được, và chỉ sau khi đã nộp.")
    public ExplanationResponse explain(
            @AuthenticationPrincipal JwtService.AuthenticatedUser current,
            @PathVariable UUID attemptId,
            @PathVariable UUID answerId) {
        return attemptService.explain(attemptId, answerId, current);
    }

    @GetMapping("/attempts/{attemptId}/grading")
    @Operation(summary = "Chủ quiz đọc phần tự luận của một bài làm để chấm tay (FR-30). "
            + "Chỉ câu tự luận, chỉ trên quiz mình sở hữu; bài của quiz người khác trả 404.")
    public GradingViewResponse gradingView(
            @AuthenticationPrincipal JwtService.AuthenticatedUser current,
            @PathVariable UUID attemptId) {
        return attemptService.gradingView(attemptId, current);
    }

    @PatchMapping("/attempts/{attemptId}/answers/{answerId}/grade")
    @Operation(summary = "Chủ quiz chấm tay, ghi đè điểm AI (FR-30). "
            + "Điểm bị ép về khoảng [0, điểm tối đa của câu].")
    public AttemptDetailResponse overrideGrade(
            @AuthenticationPrincipal JwtService.AuthenticatedUser current,
            @PathVariable UUID attemptId,
            @PathVariable UUID answerId,
            @Valid @RequestBody OverrideGradeRequest request) {
        return attemptService.overrideGrade(attemptId, answerId, request, current);
    }

    @GetMapping("/attempts")
    @Operation(summary = "Lịch sử làm bài của tôi; lọc theo quizId nếu cần")
    public PageResponse<AttemptSummaryResponse> history(
            @AuthenticationPrincipal JwtService.AuthenticatedUser current,
            @RequestParam(required = false) UUID quizId,
            @PageableDefault(size = 20) Pageable pageable) {
        return attemptService.history(current.id(), quizId, pageable);
    }

    @GetMapping("/quizzes/{quizId}/leaderboard")
    @Operation(summary = "Bảng xếp hạng quiz — mỗi người một bài tốt nhất, tối đa 50 dòng")
    public List<LeaderboardEntryResponse> leaderboard(
            @AuthenticationPrincipal JwtService.AuthenticatedUser current,
            @PathVariable UUID quizId) {
        return attemptService.leaderboard(quizId, current);
    }
}
