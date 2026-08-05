package com.datn.quizai.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(

        @NotBlank(message = "Tên hiển thị không được để trống")
        @Size(max = 100, message = "Tên hiển thị tối đa 100 ký tự")
        String displayName,

        @Size(max = 500, message = "Đường dẫn ảnh đại diện tối đa 500 ký tự")
        String avatarUrl
) {
}
