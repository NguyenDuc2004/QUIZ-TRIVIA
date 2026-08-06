package com.datn.quizai.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Xin mã OTP đặt lại mật khẩu. */
public record ForgotPasswordRequest(
        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không đúng định dạng")
        String email
) {
}
