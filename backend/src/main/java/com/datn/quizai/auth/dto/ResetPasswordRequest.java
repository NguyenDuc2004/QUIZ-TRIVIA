package com.datn.quizai.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Đặt lại mật khẩu bằng mã OTP nhận qua email. */
public record ResetPasswordRequest(
        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không đúng định dạng")
        String email,

        @NotBlank(message = "Nhập mã xác thực trong email")
        String otp,

        @NotBlank(message = "Mật khẩu mới không được để trống")
        @Size(min = 8, message = "Mật khẩu tối thiểu 8 ký tự")
        String newPassword
) {
}
