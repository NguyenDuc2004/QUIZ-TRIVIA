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

    @GetMapping("/status")
    @Operation(summary = "Dịch vụ AI đã cấu hình chưa — giao diện dựa vào đây để báo trước, "
            + "thay vì để người dùng bấm rồi mới nhận lỗi")
    public Map<String, Object> status() {
        return Map.of(
                "available", aiOrchestrator.isAvailable(),
                "providers", aiOrchestrator.availableProviders());
    }

    // ------------------------------------------------------------- học liệu

    @GetMapping("/materials")
    @Operation(summary = "Học liệu của tôi")
    public PageResponse<MaterialResponse> listMaterials(
            @AuthenticationPrincipal JwtService.AuthenticatedUser current,
            @PageableDefault(size = 20) Pageable pageable) {
        return materialService.listMine(current.id(), pageable);
    }

    @GetMapping("/materials/{id}")
    @Operation(summary = "Chi tiết một học liệu (dùng để hỏi lại trạng thái xử lý)")
    public MaterialResponse getMaterial(@AuthenticationPrincipal JwtService.AuthenticatedUser current,
                                        @PathVariable UUID id) {
        return materialService.get(id, current);
    }

    @PostMapping("/materials")
    @Operation(summary = "Nạp học liệu bằng văn bản dán trực tiếp; xử lý chạy nền")
    public ResponseEntity<MaterialResponse> createMaterial(
            @AuthenticationPrincipal JwtService.AuthenticatedUser current,
            @Valid @RequestBody CreateMaterialRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(materialService.createFromText(request, current.id()));
    }

    @PostMapping(value = "/materials/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Nạp học liệu từ file PDF/DOCX/TXT; Tika trích text rồi xử lý nền")
    public ResponseEntity<MaterialResponse> uploadMaterial(
            @AuthenticationPrincipal JwtService.AuthenticatedUser current,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String topic) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(materialService.createFromFile(file, title, topic, current.id()));
    }

    @PatchMapping("/materials/{id}/shared")
    @Operation(summary = "Bật/tắt chia sẻ học liệu cho người học (features/08). Chỉ tài liệu đã bật "
            + "mới được trợ lý AI dùng để trả lời cho người khác; tài liệu chưa xử lý xong trả 409.")
    public MaterialResponse setMaterialShared(
            @AuthenticationPrincipal JwtService.AuthenticatedUser current,
            @PathVariable UUID id,
            @RequestParam boolean shared) {
        return materialService.setShared(id, shared, current);
    }

    @DeleteMapping("/materials/{id}")
    @Operation(summary = "Xoá học liệu và toàn bộ vector của nó")
    public ResponseEntity<Void> deleteMaterial(@AuthenticationPrincipal JwtService.AuthenticatedUser current,
                                               @PathVariable UUID id) {
        materialService.delete(id, current);
        return ResponseEntity.noContent().build();
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
