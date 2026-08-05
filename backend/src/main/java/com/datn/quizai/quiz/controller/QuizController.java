package com.datn.quizai.quiz.controller;

import com.datn.quizai.auth.service.JwtService;
import com.datn.quizai.common.dto.PageResponse;
import com.datn.quizai.quiz.domain.Difficulty;
import com.datn.quizai.quiz.dto.QuizDetailResponse;
import com.datn.quizai.quiz.dto.QuizRequest;
import com.datn.quizai.quiz.dto.QuizSummaryResponse;
import com.datn.quizai.quiz.dto.SetQuizQuestionsRequest;
import com.datn.quizai.quiz.service.QuizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Quản lý quiz — docs/api.md §3.
 * <p>
 * Guest chỉ gọi được hai endpoint đọc: danh sách quiz công khai và trang giới thiệu một quiz
 * (không kèm câu hỏi). Mọi endpoint ghi yêu cầu vai trò CREATOR/ADMIN và quyền sở hữu.
 */
@RestController
@RequestMapping("/api/v1/quizzes")
@Tag(name = "Quiz", description = "Tạo và quản lý quiz")
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @GetMapping
    @Operation(summary = "Danh sách quiz công khai; truyền mine=true để lấy quiz của tôi (cần đăng nhập)")
    public PageResponse<QuizSummaryResponse> list(
            @AuthenticationPrincipal JwtService.AuthenticatedUser current,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) Difficulty difficulty,
            @RequestParam(required = false, name = "q") String keyword,
            @RequestParam(required = false, defaultValue = "false") boolean mine,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        if (!mine) {
            return quizService.listPublic(categoryId, difficulty, keyword, pageable);
        }
        return quizService.listMine(requireLogin(current).id(), categoryId, difficulty, keyword, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Thông tin giới thiệu quiz (không kèm câu hỏi) — Guest xem được nếu quiz công khai")
    public QuizSummaryResponse getSummary(@AuthenticationPrincipal JwtService.AuthenticatedUser current,
                                          @PathVariable UUID id) {
        return quizService.getSummary(id, current);
    }

    @GetMapping("/{id}/questions")
    @Operation(summary = "Câu hỏi trong quiz kèm đáp án đúng — chỉ chủ sở hữu/Admin")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('CREATOR', 'ADMIN')")
    public QuizDetailResponse getDetail(@AuthenticationPrincipal JwtService.AuthenticatedUser current,
                                        @PathVariable UUID id) {
        return quizService.getDetailForEditing(id, current);
    }

    @PostMapping
    @Operation(summary = "Tạo quiz mới (mặc định PRIVATE)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('CREATOR', 'ADMIN')")
    public ResponseEntity<QuizSummaryResponse> create(
            @AuthenticationPrincipal JwtService.AuthenticatedUser current,
            @Valid @RequestBody QuizRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(quizService.create(request, current.id()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật quiz (chỉ chủ sở hữu/Admin)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('CREATOR', 'ADMIN')")
    public QuizSummaryResponse update(@AuthenticationPrincipal JwtService.AuthenticatedUser current,
                                      @PathVariable UUID id,
                                      @Valid @RequestBody QuizRequest request) {
        return quizService.update(id, request, current);
    }

    @PutMapping("/{id}/questions")
    @Operation(summary = "Đặt lại danh sách & thứ tự câu hỏi của quiz (thay thế toàn bộ)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('CREATOR', 'ADMIN')")
    public QuizDetailResponse setQuestions(@AuthenticationPrincipal JwtService.AuthenticatedUser current,
                                           @PathVariable UUID id,
                                           @Valid @RequestBody SetQuizQuestionsRequest request) {
        return quizService.setQuestions(id, request.questionIds(), current);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa quiz (câu hỏi vẫn còn trong ngân hàng)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('CREATOR', 'ADMIN')")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal JwtService.AuthenticatedUser current,
                                       @PathVariable UUID id) {
        quizService.delete(id, current);
        return ResponseEntity.noContent().build();
    }

    /**
     * `GET /quizzes` mở cho Guest nên khi họ truyền {@code mine=true} phải chặn tại đây,
     * không thể dựa vào filter bảo mật.
     */
    private JwtService.AuthenticatedUser requireLogin(JwtService.AuthenticatedUser current) {
        if (current == null) {
            throw com.datn.quizai.common.exception.BusinessException
                    .unauthorized("Bạn cần đăng nhập để xem quiz của mình");
        }
        return current;
    }
}
