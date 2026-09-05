package com.datn.quizai.ai.controller;

import com.datn.quizai.ai.provider.AiOrchestrator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Dịch vụ AI đã cấu hình chưa — giao diện dựa vào đây để báo trước, thay vì để người dùng bấm rồi mới
 * nhận lỗi.
 *
 * <h3>Vì sao tách khỏi {@code AiController}</h3>
 * Lớp kia gắn {@code @PreAuthorize("hasAnyRole('CREATOR','ADMIN')")} cấp lớp, nên endpoint này trả
 * <b>403</b> với người học. Từ 04/09/2026 người học nạp được học liệu, mà việc nạp phụ thuộc vào đúng
 * dịch vụ AI này (embedding). Hệ quả trước khi sửa: trang Học liệu của người học <b>không bao giờ</b>
 * hiện được cảnh báo "chưa cấu hình API key" — họ tải tệp lên, tệp dừng ở trạng thái Lỗi, và không có
 * gì cho biết vì sao. Một request 403 lặng lẽ mỗi lần mở trang.
 *
 * Tách sang lớp riêng chứ không mở lẻ một phương thức trong lớp đang khoá cả cụm — cùng lý do đã ghi ở
 * {@link MaterialController}: một lớp có luật thống nhất thì đọc là biết.
 *
 * <h3>Có lộ gì không</h3>
 * Chỉ hai thứ: dịch vụ có chạy được không, và tên các nhà cung cấp đã cấu hình ({@code gemini},
 * {@code groq}). Không có khoá, không có hạn mức, không có số liệu sử dụng — những thứ đó nằm ở khu
 * quản trị và vẫn khoá nguyên.
 */
@RestController
@RequestMapping("/api/v1/ai/status")
@Tag(name = "AI", description = "Học liệu RAG và sinh đề bằng AI")
@SecurityRequirement(name = "bearerAuth")
public class AiStatusController {

    private final AiOrchestrator aiOrchestrator;

    public AiStatusController(AiOrchestrator aiOrchestrator) {
        this.aiOrchestrator = aiOrchestrator;
    }

    @GetMapping
    @Operation(summary = "Dịch vụ AI đã cấu hình chưa — giao diện dựa vào đây để báo trước, "
            + "thay vì để người dùng bấm rồi mới nhận lỗi. Mọi tài khoản đã đăng nhập đều gọi được.")
    public Map<String, Object> status() {
        return Map.of(
                "available", aiOrchestrator.isAvailable(),
                "providers", aiOrchestrator.availableProviders());
    }
}
