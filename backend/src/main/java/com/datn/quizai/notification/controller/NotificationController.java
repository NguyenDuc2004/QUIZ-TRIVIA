package com.datn.quizai.notification.controller;

import com.datn.quizai.auth.service.JwtService;
import com.datn.quizai.common.dto.PageResponse;
import com.datn.quizai.notification.dto.NotificationResponse;
import com.datn.quizai.notification.dto.NotificationSettingsResponse;
import com.datn.quizai.notification.dto.UpdateSettingsRequest;
import com.datn.quizai.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * Trung tâm thông báo (features/16, FR-68 & FR-70).
 * <p>
 * <b>Mọi endpoint chỉ làm việc trên thông báo của chính người gọi.</b> Không có tham số {@code userId} ở bất
 * kỳ đâu — id lấy từ token. Thiếu nguyên tắc đó thì một tham số duy nhất là đủ để đọc thông báo của người khác,
 * và thông báo chứa thông tin riêng (tiến độ học, thành tích).
 * <p>
 * <b>Không có endpoint tạo thông báo.</b> Thông báo chỉ sinh từ sự kiện thật trong hệ thống hoặc từ job nền.
 * Mở một đường ghi qua API là mở đường để gửi thông báo cho người khác — và đó là kênh spam sẵn có.
 */
@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "Trung tâm thông báo và nhắc ôn tập")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Thông báo của tôi, mới nhất trước")
    public PageResponse<NotificationResponse> danhSach(
            @AuthenticationPrincipal JwtService.AuthenticatedUser current,
            @PageableDefault(size = 20) Pageable pageable) {

        return PageResponse.of(service.danhSach(current.id(), pageable), NotificationResponse::from);
    }

    /**
     * Số chưa đọc, cho chấm đỏ trên chuông.
     * <p>
     * Có endpoint riêng thay vì bắt gọi {@code GET /notifications} rồi tự đếm: chấm đỏ hiện ở <i>mọi</i>
     * trang, nên nó sẽ là truy vấn chạy nhiều nhất của tính năng này. Kéo về 20 thông báo đầy đủ chỉ để lấy
     * một con số là tốn vô ích, và con số đó còn sai nếu có hơn 20 cái chưa đọc.
     */
    @GetMapping("/unread-count")
    @Operation(summary = "Số thông báo chưa đọc")
    public Map<String, Long> soChuaDoc(@AuthenticationPrincipal JwtService.AuthenticatedUser current) {
        return Map.of("soChuaDoc", service.soChuaDoc(current.id()));
    }

    @PutMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Đánh dấu một thông báo đã đọc. Thông báo của người khác coi như không tồn tại.")
    public void danhDauDaDoc(@AuthenticationPrincipal JwtService.AuthenticatedUser current,
                             @PathVariable UUID id) {
        service.danhDauDaDoc(id, current.id());
    }

    @PutMapping("/read-all")
    @Operation(summary = "Đánh dấu tất cả đã đọc; trả về số dòng vừa đổi")
    public Map<String, Integer> danhDauTatCa(
            @AuthenticationPrincipal JwtService.AuthenticatedUser current) {
        return Map.of("daDanhDau", service.danhDauTatCaDaDoc(current.id()));
    }

    @GetMapping("/settings")
    @Operation(summary = "Cài đặt loại thông báo, kèm danh sách loại điều chỉnh được")
    public NotificationSettingsResponse caiDat(
            @AuthenticationPrincipal JwtService.AuthenticatedUser current) {
        return NotificationSettingsResponse.of(service.loaiBiTat(current.id()));
    }

    @PutMapping("/settings")
    @Operation(summary = "Đặt lại danh sách loại bị tắt. Loại SYSTEM không tắt được và bị bỏ qua.")
    public NotificationSettingsResponse capNhatCaiDat(
            @AuthenticationPrincipal JwtService.AuthenticatedUser current,
            @Valid @RequestBody UpdateSettingsRequest request) {

        return NotificationSettingsResponse.of(service.capNhatCaiDat(current.id(), request.safe()));
    }
}
