package com.datn.quizai.user.dto;

import com.datn.quizai.user.Role;
import com.datn.quizai.user.User;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Hồ sơ người dùng trả ra API — KHÔNG bao giờ chứa `passwordHash`. */
public record UserResponse(
        UUID id,
        String email,
        String displayName,
        String avatarUrl,
        Role role,
        OffsetDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getRole(),
                user.getCreatedAt());
    }
}
