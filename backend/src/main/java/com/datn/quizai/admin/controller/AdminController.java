package com.datn.quizai.admin.controller;

import com.datn.quizai.admin.dto.AdminUserResponse;
import com.datn.quizai.admin.dto.AiUsageSummary;
import com.datn.quizai.admin.repository.AiUsageRepository;
import com.datn.quizai.admin.service.AdminUserService;
import com.datn.quizai.auth.service.JwtService;
import com.datn.quizai.common.dto.PageResponse;
import com.datn.quizai.user.domain.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    private final AiUsageRepository aiUsageRepository;

    public AdminController(AdminUserService adminUserService, AiUsageRepository aiUsageRepository) {
        this.adminUserService = adminUserService;
        this.aiUsageRepository = aiUsageRepository;
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

    @GetMapping("/ai/usage")
    @Operation(summary = "Tổng hợp chi phí, độ tin cậy và độ trễ của các lời gọi mô hình trong N ngày "
            + "gần nhất. KHÔNG trả về khoá API hay nội dung prompt.")
    public AiUsageSummary aiUsage(
            @RequestParam(defaultValue = "30") @Min(1) @Max(365) int days) {
        return aiUsageRepository.summary(days);
    }
}
