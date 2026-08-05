package com.datn.quizai.auth.dto;

import com.datn.quizai.user.dto.UserResponse;

/**
 * Kết quả đăng ký / đăng nhập / làm mới token.
 *
 * @param expiresIn số giây còn hiệu lực của access token
 */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserResponse user
) {
    public static AuthResponse of(String accessToken, String refreshToken, long expiresIn, UserResponse user) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresIn, user);
    }
}
