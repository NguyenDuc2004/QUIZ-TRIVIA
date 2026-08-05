package com.datn.quizai.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(

        @NotBlank(message = "Refresh token không được để trống")
        String refreshToken
) {
}
