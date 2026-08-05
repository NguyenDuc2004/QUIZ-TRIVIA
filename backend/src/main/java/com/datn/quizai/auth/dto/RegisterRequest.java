package com.datn.quizai.auth.dto;

import com.datn.quizai.user.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không đúng định dạng")
        @Size(max = 255, message = "Email tối đa 255 ký tự")
        String email,

        @NotBlank(message = "Mật khẩu không được để trống")
        @Size(min = 8, max = 72, message = "Mật khẩu phải từ 8 đến 72 ký tự")
        String password,

        @NotBlank(message = "Tên hiển thị không được để trống")
        @Size(max = 100, message = "Tên hiển thị tối đa 100 ký tự")
        String displayName,

        /** Cho phép chọn LEARNER hoặc CREATOR khi đăng ký; bỏ trống → LEARNER. ADMIN không tự đăng ký. */
        Role role
) {
}
