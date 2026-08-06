package com.datn.quizai.realtime.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Khách vãng lai vào phòng bằng mã PIN hoặc QR.
 *
 * @param avatar mã avatar trong {@code PlayerAvatar}; bỏ trống thì server bốc ngẫu nhiên
 */
public record JoinAsGuestRequest(
        @NotBlank(message = "Nhập biệt danh để mọi người nhận ra bạn")
        @Size(max = 30, message = "Biệt danh tối đa 30 ký tự")
        String displayName,

        String avatar
) {
}
