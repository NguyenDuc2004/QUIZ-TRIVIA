package com.datn.quizai.chat.dto;

import com.datn.quizai.chat.domain.ChatSession;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Một phiên trong danh sách bên trái màn trợ lý. */
public record ChatSessionResponse(
        UUID id,
        String title,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static ChatSessionResponse from(ChatSession session) {
        return new ChatSessionResponse(session.getId(), session.getTitle(),
                session.getCreatedAt(), session.getUpdatedAt());
    }
}
