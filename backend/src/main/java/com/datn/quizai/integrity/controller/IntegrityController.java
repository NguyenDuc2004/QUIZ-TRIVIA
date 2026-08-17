package com.datn.quizai.integrity.controller;

import com.datn.quizai.attempt.domain.QuizAttempt;
import com.datn.quizai.attempt.repository.QuizAttemptRepository;
import com.datn.quizai.auth.service.JwtService;
import com.datn.quizai.common.exception.BusinessException;
import com.datn.quizai.integrity.domain.AttemptIntegrity;
import com.datn.quizai.integrity.domain.ProctoringEvent;
import com.datn.quizai.integrity.domain.ReviewStatus;
import com.datn.quizai.integrity.dto.IntegrityReport;
import com.datn.quizai.integrity.dto.ProctoringEventsRequest;
import com.datn.quizai.integrity.dto.ReviewRequest;
import com.datn.quizai.integrity.repository.AttemptIntegrityRepository;
import com.datn.quizai.integrity.repository.ProctoringEventRepository;
import com.datn.quizai.integrity.service.IntegrityService;
import com.datn.quizai.integrity.service.ProctoringService;
import com.datn.quizai.integrity.service.RiskScorer;
import com.datn.quizai.user.domain.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tín hiệu hành vi và báo cáo tính toàn vẹn (features/12).
 *
 * <h3>Ai xem được báo cáo</h3>
 * <b>Chủ quiz</b> và <b>Admin</b>. Người làm bài <b>không</b> xem được điểm rủi ro của chính mình: biết chính
 * xác tín hiệu nào bị tính và nặng bao nhiêu là biết cách tránh, và khi đó cả cơ chế chỉ còn lọc được người
 * không biết nó tồn tại.
 * <p>
 * Nhưng việc <i>có thu tín hiệu</i> thì phải nói rõ với người làm bài trước khi vào thi — minh bạch là ràng
 * buộc của đặc tả. Giao diện nói ở màn giới thiệu đề, không phải ẩn đi.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Integrity", description = "Chống gian lận thi: tín hiệu hành vi và báo cáo tính toàn vẹn")
@SecurityRequirement(name = "bearerAuth")
public class IntegrityController {

    private final ProctoringService proctoringService;
    private final IntegrityService integrityService;
    private final AttemptIntegrityRepository integrityRepository;
    private final ProctoringEventRepository eventRepository;
    private final QuizAttemptRepository attemptRepository;

    public IntegrityController(ProctoringService proctoringService,
                               IntegrityService integrityService,
                               AttemptIntegrityRepository integrityRepository,
                               ProctoringEventRepository eventRepository,
                               QuizAttemptRepository attemptRepository) {
        this.proctoringService = proctoringService;
        this.integrityService = integrityService;
        this.integrityRepository = integrityRepository;
        this.eventRepository = eventRepository;
        this.attemptRepository = attemptRepository;
    }

    @PostMapping("/attempts/{id}/proctoring-events")
    @Operation(summary = "Gửi lô tín hiệu hành vi (tối đa 50 mỗi lô). CHỈ chấp nhận lượt thi EXAM của chính "
            + "mình. Server KHÔNG nhận nội dung đã dán — chỉ độ dài.")
    public Map<String, Integer> guiSuKien(@AuthenticationPrincipal JwtService.AuthenticatedUser current,
                                          @PathVariable UUID id,
                                          @Valid @RequestBody ProctoringEventsRequest request) {
        return Map.of("soSuKienDaGhi", proctoringService.ghiNhan(id, current.id(), request));
    }

    @GetMapping("/attempts/{id}/integrity")
    @Operation(summary = "Báo cáo tính toàn vẹn của một lượt thi — chỉ chủ quiz hoặc Admin. Người làm bài "
            + "không xem được điểm rủi ro của chính mình.")
    @Transactional(readOnly = true)
    public IntegrityReport baoCao(@AuthenticationPrincipal JwtService.AuthenticatedUser current,
                                  @PathVariable UUID id) {
        QuizAttempt attempt = requireQuyenXem(id, current);
        AttemptIntegrity integrity = integrityRepository.findByAttemptId(id)
                .orElseThrow(() -> BusinessException.notFound(
                        "Lượt này chưa có báo cáo tính toàn vẹn — có thể là lượt luyện tập"));

        List<ProctoringEvent> events = eventRepository.findByAttemptIdOrderByOccurredAt(id);
        return new IntegrityReport(
                id,
                attempt.getQuiz() == null ? "?" : attempt.getQuiz().getTitle(),
                attempt.getUser() == null ? "?" : attempt.getUser().getDisplayName(),
                integrity.getRiskScore(),
                integrity.getRiskScore() >= RiskScorer.NGUONG_GAN_CO,
                integrityService.docCo(integrity),
                integrity.getAiNote(),
                integrity.getReviewStatus(),
                integrity.getReviewedAt(),
                integrity.getReviewNote(),
                events.size(),
                events.stream()
                        .map(e -> new IntegrityReport.SuKien(
                                e.getEventType().name(), e.getDetail(), e.getOccurredAt()))
                        .toList(),
                CANH_BAO);
    }

    @PutMapping("/attempts/{id}/integrity/review")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Kết luận hợp lệ / không hợp lệ. Chỉ chủ quiz hoặc Admin. Hệ thống không bao giờ tự "
            + "kết luận — trạng thái ban đầu luôn là PENDING.")
    @Transactional
    public IntegrityReport raSoat(@AuthenticationPrincipal JwtService.AuthenticatedUser current,
                                  @PathVariable UUID id,
                                  @Valid @RequestBody ReviewRequest request) {
        requireQuyenXem(id, current);

        if (request.status() == ReviewStatus.PENDING) {
            throw BusinessException.badRequest(
                    "PENDING là trạng thái ban đầu của hệ thống, không phải một kết luận");
        }
        AttemptIntegrity integrity = integrityRepository.findByAttemptId(id)
                .orElseThrow(() -> BusinessException.notFound("Lượt này chưa có báo cáo tính toàn vẹn"));

        integrity.setReviewStatus(request.status());
        integrity.setReviewedBy(current.id());
        integrity.setReviewedAt(OffsetDateTime.now());
        integrity.setReviewNote(request.note());
        integrityRepository.save(integrity);

        return baoCao(current, id);
    }

    @GetMapping("/admin/integrity/flagged")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Bài thi bị gắn cờ, điểm rủi ro cao trước. Lọc theo trạng thái rà soát; mặc định "
            + "chỉ hiện bài CHƯA ai xem — đó là hàng chờ việc, không phải danh sách lịch sử.")
    @Transactional(readOnly = true)
    public com.datn.quizai.common.dto.PageResponse<IntegrityReport> danhSachGanCo(
            @AuthenticationPrincipal JwtService.AuthenticatedUser current,
            @org.springframework.web.bind.annotation.RequestParam(required = false) ReviewStatus status,
            @org.springframework.data.web.PageableDefault(size = 20)
            org.springframework.data.domain.Pageable pageable) {
        ReviewStatus loc = status == null ? ReviewStatus.PENDING : status;
        var trang = integrityRepository.findFlagged(RiskScorer.NGUONG_GAN_CO, loc, pageable);

        return com.datn.quizai.common.dto.PageResponse.of(trang, integrity -> {
            var attempt = attemptRepository.findById(integrity.getAttemptId()).orElse(null);
            return new IntegrityReport(
                    integrity.getAttemptId(),
                    attempt == null || attempt.getQuiz() == null ? "?" : attempt.getQuiz().getTitle(),
                    attempt == null || attempt.getUser() == null ? "?" : attempt.getUser().getDisplayName(),
                    integrity.getRiskScore(),
                    true,
                    integrityService.docCo(integrity),
                    integrity.getAiNote(),
                    integrity.getReviewStatus(),
                    integrity.getReviewedAt(),
                    integrity.getReviewNote(),
                    eventRepository.countByAttemptId(integrity.getAttemptId()),
                    // Danh sách không kèm từng sự kiện: trang này chỉ để chọn bài cần xem, và kéo hàng trăm
                    // dòng sự kiện cho hai mươi bài là tốn vô ích. Mở một bài mới lấy chi tiết.
                    List.of(),
                    CANH_BAO);
        });
    }

    /**
     * Chủ quiz hoặc Admin mới xem được.
     * <p>
     * Trả 404 chứ không 403 với người không có quyền: 403 là xác nhận lượt thi đó tồn tại và đang bị gắn cờ —
     * một thông tin không nên tiết lộ.
     */
    private QuizAttempt requireQuyenXem(UUID attemptId, JwtService.AuthenticatedUser current) {
        QuizAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy lượt làm bài"));

        boolean laAdmin = current.role() == Role.ADMIN;
        boolean laChuQuiz = attempt.getQuiz() != null && attempt.getQuiz().getOwner() != null
                && attempt.getQuiz().getOwner().getId().equals(current.id());

        if (!laAdmin && !laChuQuiz) {
            throw BusinessException.notFound("Không tìm thấy lượt làm bài");
        }
        return attempt;
    }

    /** Câu nhắc luôn kèm theo báo cáo — xem javadoc của lớp. */
    private static final String CANH_BAO =
            "Tín hiệu hành vi thu từ trình duyệt nên có thể bị chặn hoặc giả mạo, và mỗi tín hiệu đều có cách "
            + "giải thích vô hại. Điểm rủi ro là gợi ý để rà soát, KHÔNG phải bằng chứng gian lận. Hệ thống "
            + "không tự động xử lý bài nào.";
}
