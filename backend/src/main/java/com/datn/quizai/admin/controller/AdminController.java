package com.datn.quizai.admin.controller;

import com.datn.quizai.admin.dto.AdminCategoryResponse;
import com.datn.quizai.admin.dto.AdminUserResponse;
import com.datn.quizai.admin.dto.AiConfigResponse;
import com.datn.quizai.admin.dto.AiUsageSummary;
import com.datn.quizai.admin.dto.CategoryRequest;
import com.datn.quizai.admin.dto.LiveRoomResponse;
import com.datn.quizai.admin.dto.SystemOverview;
import com.datn.quizai.admin.repository.AiUsageRepository;
import com.datn.quizai.admin.repository.OverviewRepository;
import com.datn.quizai.admin.service.AdminAiConfigService;
import com.datn.quizai.admin.service.AdminContentService;
import com.datn.quizai.admin.service.AdminRoomService;
import com.datn.quizai.admin.service.AdminUserService;
import com.datn.quizai.auth.service.JwtService;
import com.datn.quizai.common.dto.PageResponse;
import com.datn.quizai.quiz.dto.QuizSummaryResponse;
import com.datn.quizai.user.domain.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Khu vực quản trị (features/10).
 * <p>
 * {@code @PreAuthorize} đặt ở <b>cấp lớp</b>: mọi endpoint trong đây đều chỉ dành cho ADMIN, không có
 * ngoại lệ nào. Gắn ở cấp lớp thì thêm endpoint mới cũng tự được bảo vệ — gắn ở từng phương thức thì
 * quên một chỗ là mở toang một chỗ, mà đúng lớp này là nơi không được phép quên.
 * <p>
 * <b>Không có endpoint xoá người dùng.</b> Xem lý do ở {@link AdminUserService}.
 */
@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin", description = "Quản lý người dùng và giám sát chi phí AI")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminUserService adminUserService;
    private final AdminContentService adminContentService;
    private final AdminRoomService adminRoomService;
    private final AdminAiConfigService adminAiConfigService;
    private final AiUsageRepository aiUsageRepository;
    private final OverviewRepository overviewRepository;

    public AdminController(AdminUserService adminUserService,
                           AdminContentService adminContentService,
                           AdminRoomService adminRoomService,
                           AdminAiConfigService adminAiConfigService,
                           AiUsageRepository aiUsageRepository,
                           OverviewRepository overviewRepository) {
        this.adminUserService = adminUserService;
        this.adminContentService = adminContentService;
        this.adminRoomService = adminRoomService;
        this.adminAiConfigService = adminAiConfigService;
        this.aiUsageRepository = aiUsageRepository;
        this.overviewRepository = overviewRepository;
    }

    @GetMapping("/overview")
    @Operation(summary = "Tổng quan hệ thống: KPI người dùng / nội dung / hoạt động / chi phí AI tháng "
            + "này, kèm dữ liệu ba biểu đồ. Mọi con số đến từ dữ liệu thật, không ước lượng.")
    public SystemOverview overview(@RequestParam(defaultValue = "14") @Min(7) @Max(90) int days) {
        return overviewRepository.overview(days);
    }

    @PostMapping("/users/{id}/revoke")
    @Operation(summary = "Thu hồi mọi phiên đăng nhập của một người dùng NHƯNG không khoá tài khoản — "
            + "dùng khi nghi tài khoản bị chiếm dụng hoặc người dùng báo mất máy. Trả về số phiên đã thu hồi.")
    public Map<String, Integer> revokeSessions(@PathVariable UUID id) {
        return Map.of("soPhienDaThuHoi", adminUserService.revokeSessions(id));
    }

    @GetMapping("/users")
    @Operation(summary = "Danh sách người dùng, lọc theo từ khoá (email hoặc tên), vai trò, trạng thái khoá")
    public PageResponse<AdminUserResponse> users(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean locked,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return adminUserService.search(keyword, role, locked, pageable);
    }

    @PutMapping("/users/{id}/role")
    @Operation(summary = "Đổi vai trò người dùng. Thu hồi mọi phiên của họ vì vai trò nằm trong token. "
            + "Không thể tự hạ vai trò của chính mình.")
    public AdminUserResponse changeRole(
            @AuthenticationPrincipal JwtService.AuthenticatedUser current,
            @PathVariable UUID id,
            @RequestParam Role role) {
        return adminUserService.changeRole(id, role, current.id());
    }

    @PutMapping("/users/{id}/locked")
    @Operation(summary = "Khoá hoặc mở khoá tài khoản. Khoá thì thu hồi mọi phiên ngay, và người đó "
            + "không đăng nhập lại được. Dữ liệu của họ giữ nguyên. Không thể tự khoá chính mình.")
    public AdminUserResponse setLocked(
            @AuthenticationPrincipal JwtService.AuthenticatedUser current,
            @PathVariable UUID id,
            @RequestParam boolean locked) {
        return adminUserService.setLocked(id, locked, current.id());
    }

    // ----- Nội dung: danh mục và quiz công khai (FR-79, FR-80) -----

    @GetMapping("/categories")
    @Operation(summary = "Danh mục kèm số quiz đang dùng mỗi danh mục")
    public List<AdminCategoryResponse> categories() {
        return adminContentService.categories();
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Thêm danh mục. Đường dẫn (slug) để trống thì tự sinh từ tên, có bỏ dấu tiếng Việt.")
    public AdminCategoryResponse createCategory(@Valid @RequestBody CategoryRequest request) {
        return adminContentService.createCategory(request);
    }

    @PutMapping("/categories/{id}")
    @Operation(summary = "Sửa tên, đường dẫn hoặc mô tả danh mục")
    public AdminCategoryResponse updateCategory(@PathVariable UUID id,
                                                @Valid @RequestBody CategoryRequest request) {
        return adminContentService.updateCategory(id, request);
    }

    @DeleteMapping("/categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Xoá danh mục. Trả 409 kèm số lượng nếu còn quiz đang dùng — không xoá kèm quiz "
            + "và cũng không để quiz mồ côi.")
    public void deleteCategory(@PathVariable UUID id) {
        adminContentService.deleteCategory(id);
    }

    @GetMapping("/quizzes")
    @Operation(summary = "Quiz công khai để kiểm duyệt — đúng danh sách người học nhìn thấy. Quiz riêng "
            + "tư không nằm trong đây.")
    public PageResponse<QuizSummaryResponse> quizzes(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UUID categoryId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return adminContentService.publicQuizzes(keyword, categoryId, pageable);
    }

    @PutMapping("/quizzes/{id}/hide")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Ẩn quiz vi phạm: đưa về riêng tư, KHÔNG xoá. Chủ quiz sửa lại rồi công khai "
            + "lại được; lượt làm bài và bảng xếp hạng cũ giữ nguyên.")
    public void hideQuiz(@AuthenticationPrincipal JwtService.AuthenticatedUser current,
                         @PathVariable UUID id) {
        adminContentService.hideQuiz(id, current.id());
    }

    // ----- Phòng đấu đang chạy (FR-81, FR-82) -----

    @GetMapping("/rooms")
    @Operation(summary = "Phòng đang chờ hoặc đang chơi, ghép metadata PostgreSQL với trạng thái Redis. "
            + "Cờ `treo` bật khi bản ghi còn mà trạng thái Redis đã mất — phòng không ai chơi được.")
    public List<LiveRoomResponse> rooms() {
        return adminRoomService.liveRooms();
    }

    @PostMapping("/rooms/{roomCode}/close")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cưỡng chế đóng phòng: xoá trạng thái Redis VÀ chuyển bản ghi sang FINISHED. "
            + "Dùng cho phòng treo hoặc phòng vi phạm.")
    public void closeRoom(@AuthenticationPrincipal JwtService.AuthenticatedUser current,
                          @PathVariable String roomCode) {
        adminRoomService.forceClose(roomCode, current.id());
    }

    // ----- Giám sát AI (FR-83) -----

    @GetMapping("/ai/config")
    @Operation(summary = "Trạng thái cấu hình các nhà cung cấp AI: đã có khoá hay chưa, có nằm trong thứ "
            + "tự ưu tiên hay không. CHỈ true/false — không trả giá trị khoá, không trả system prompt.")
    public AiConfigResponse aiConfig() {
        return adminAiConfigService.config();
    }

    @GetMapping("/ai/usage")
    @Operation(summary = "Tổng hợp chi phí, độ tin cậy và độ trễ của các lời gọi mô hình trong N ngày "
            + "gần nhất. KHÔNG trả về khoá API hay nội dung prompt.")
    public AiUsageSummary aiUsage(
            @RequestParam(defaultValue = "30") @Min(1) @Max(365) int days) {
        return aiUsageRepository.summary(days);
    }
}
