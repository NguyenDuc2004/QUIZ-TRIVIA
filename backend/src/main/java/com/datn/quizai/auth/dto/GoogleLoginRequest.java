package com.datn.quizai.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Đăng nhập bằng Google.
 *
 * @param idToken ID token do Google Identity Services cấp cho frontend. Backend tự xác minh chữ ký
 *                với Google — không tin bất cứ thông tin nào client tự khai kèm theo.
 */
public record GoogleLoginRequest(
        @NotBlank(message = "Thiếu ID token của Google")
        String idToken
) {
}
