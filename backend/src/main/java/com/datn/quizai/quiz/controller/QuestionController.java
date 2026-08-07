package com.datn.quizai.quiz.controller;

import com.datn.quizai.quiz.domain.Difficulty;
import com.datn.quizai.quiz.domain.QuestionType;
import com.datn.quizai.quiz.service.QuestionService;
import com.datn.quizai.auth.service.JwtService;
import com.datn.quizai.common.dto.PageResponse;
import com.datn.quizai.quiz.dto.QuestionRequest;
import com.datn.quizai.quiz.dto.TopicResponse;
import com.datn.quizai.quiz.dto.QuestionResponse;
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

import java.util.List;
import java.util.UUID;

/**
 * Ngân hàng câu hỏi — docs/api.md §3.
 * Toàn bộ endpoint yêu cầu vai trò CREATOR hoặc ADMIN và chỉ tác động lên câu hỏi của chính mình.
 */
@RestController
@RequestMapping("/api/v1/questions")
@Tag(name = "Câu hỏi", description = "Ngân hàng câu hỏi tái sử dụng")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('CREATOR', 'ADMIN')")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @GetMapping
    @Operation(summary = "Ngân hàng câu hỏi của tôi (lọc theo loại, độ khó, chủ đề, từ khóa)")
    public PageResponse<QuestionResponse> listMyBank(
            @AuthenticationPrincipal JwtService.AuthenticatedUser current,
            @RequestParam(required = false) QuestionType type,
            @RequestParam(required = false) Difficulty difficulty,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false, name = "q") String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return questionService.listMyBank(current.id(), type, difficulty, topic, keyword, pageable);
    }

    @GetMapping("/topics")
    @Operation(summary = "Các chủ đề trong ngân hàng của tôi, kèm số câu mỗi chủ đề. "
            + "Dùng để lọc câu hỏi và gợi ý khi soạn câu mới.")
    public List<TopicResponse> listMyTopics(@AuthenticationPrincipal JwtService.AuthenticatedUser current) {
        return questionService.listMyTopics(current.id());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết câu hỏi kèm đáp án đúng (chỉ chủ sở hữu)")
    public QuestionResponse get(@AuthenticationPrincipal JwtService.AuthenticatedUser current,
                                @PathVariable UUID id) {
        return questionService.get(id, current);
    }

    @PostMapping
    @Operation(summary = "Tạo câu hỏi mới (5 loại, luật riêng theo từng loại)")
    public ResponseEntity<QuestionResponse> create(
            @AuthenticationPrincipal JwtService.AuthenticatedUser current,
            @Valid @RequestBody QuestionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(questionService.create(request, current.id()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật câu hỏi (thay toàn bộ lựa chọn)")
    public QuestionResponse update(@AuthenticationPrincipal JwtService.AuthenticatedUser current,
                                   @PathVariable UUID id,
                                   @Valid @RequestBody QuestionRequest request) {
        return questionService.update(id, request, current);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa câu hỏi (từ chối nếu đang được dùng trong quiz)")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal JwtService.AuthenticatedUser current,
                                       @PathVariable UUID id) {
        questionService.delete(id, current);
        return ResponseEntity.noContent().build();
    }
}
