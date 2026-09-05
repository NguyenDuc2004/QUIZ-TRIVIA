package com.datn.quizai.auth.dto;

import com.datn.quizai.user.domain.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Yêu cầu tự đổi vai trò.
 *
 * <p>Chỉ nhận LEARNER hoặc CREATOR — ADMIN bị chặn ở {@code AuthService}, không phải ở đây: luật vai
 * trò phải nằm một chỗ, và chỗ đó là service. Kiểm ở DTO thì đường Google và đường đăng ký sẽ có bản
 * kiểm riêng, rồi ba bản trôi khỏi nhau.
 */
public record DoiVaiTroRequest(
        @Schema(description = "Vai trò muốn đổi sang: LEARNER hoặc CREATOR", example = "CREATOR")
        @NotNull(message = "Chưa chọn vai trò")
        Role role) {
}
