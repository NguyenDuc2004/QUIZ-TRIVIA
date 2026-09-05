package com.datn.quizai.ai.controller;

import com.datn.quizai.ai.dto.AiJobResponse;
import com.datn.quizai.ai.dto.ApproveQuestionsRequest;
import com.datn.quizai.ai.dto.CreateMaterialRequest;
import com.datn.quizai.ai.dto.GenerateQuestionsRequest;
import com.datn.quizai.ai.dto.MaterialResponse;
import com.datn.quizai.ai.provider.AiOrchestrator;
import com.datn.quizai.ai.service.AiJobService;
import com.datn.quizai.ai.service.MaterialService;
import com.datn.quizai.auth.service.JwtService;
import com.datn.quizai.common.dto.PageResponse;
import com.datn.quizai.quiz.dto.QuestionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Học liệu RAG và sinh đề bằng AI — docs/api.md §6.
 * <p>
 * Toàn bộ yêu cầu vai trò <b>CREATOR/ADMIN</b>: đây là công cụ soạn nội dung, và mỗi lời gọi đều
 * tốn tiền API nên không mở cho mọi tài khoản đăng ký được.
 * <p>
 * <b>Nhóm học liệu đã chuyển sang {@link MaterialController}</b> (04/09/2026) vì người học phải nạp
 * được tài liệu của chính họ — không thì trợ lý học tập chết hẳn với người học đơn lẻ. Chuyển sang
 * lớp riêng chứ không mở lẻ vài phương thức ở đây: một lớp cấm cả cụm rồi mở ngoại lệ bên trong là
 * cách chắc chắn để sau này có người mở quyền quá tay. Phần chi phí mà luật vai trò đang thay mặt
 * canh được thay bằng trần số tài liệu, cưỡng chế ở {@code MaterialService}.
 * <p>
 * <b>{@code GET /ai/status} cũng đã chuyển đi</b> (05/09/2026), sang {@link AiStatusController}: nó trả
 * 403 với người học, nên trang Học liệu của họ không bao giờ hiện được cảnh báo "chưa cấu hình API
 * key" — dù việc nạp tài liệu của họ phụ thuộc vào đúng dịch vụ đó.
 */
@RestController
@RequestMapping("/api/v1/ai")
@Tag(name = "AI", description = "Học liệu RAG và sinh đề bằng AI")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('CREATOR', 'ADMIN')")
public class AiController {

    private final MaterialService materialService;
    private final AiJobService jobService;
    private final AiOrchestrator aiOrchestrator;

    public AiController(MaterialService materialService,
                        AiJobService jobService,
                        AiOrchestrator aiOrchestrator) {
        this.materialService = materialService;
        this.jobService = jobService;
        this.aiOrchestrator = aiOrchestrator;
    }

    // ------------------------------------------------------------- sinh đề

    @PostMapping("/generate-questions")
    @Operation(summary = "Sinh câu hỏi (RAG hoặc theo chủ đề) — trả 202 kèm jobId, hỏi lại ở /ai/jobs/{id}")
    public ResponseEntity<AiJobResponse> generate(
            @AuthenticationPrincipal JwtService.AuthenticatedUser current,
            @Valid @RequestBody GenerateQuestionsRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(jobService.submitGeneration(request, current));
    }

    @GetMapping("/jobs/{id}")
    @Operation(summary = "Trạng thái & kết quả job")
    public AiJobResponse getJob(@AuthenticationPrincipal JwtService.AuthenticatedUser current,
                                @PathVariable UUID id) {
        return jobService.get(id, current);
    }

    @PostMapping("/jobs/{id}/approve")
    @Operation(summary = "Duyệt các câu hỏi đã chọn và lưu vào ngân hàng câu hỏi")
    public ResponseEntity<List<QuestionResponse>> approve(
            @AuthenticationPrincipal JwtService.AuthenticatedUser current,
            @PathVariable UUID id,
            @Valid @RequestBody ApproveQuestionsRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(jobService.approve(id, request, current));
    }
}
