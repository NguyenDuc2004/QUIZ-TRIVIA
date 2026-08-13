package com.datn.quizai.chat.dto;

import com.datn.quizai.chat.domain.ChatMessage;
import com.datn.quizai.chat.domain.ChatRole;
import com.datn.quizai.chat.domain.ChatSource;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Một tin nhắn khi mở lại phiên cũ.
 *
 * @param sources trích dẫn học liệu đã dùng — rỗng với tin của người dùng, và cũng rỗng với câu trả
 *                lời mà lúc đó không tìm được đoạn nào liên quan. Trường hợp thứ hai đáng để giao
 *                diện nói rõ, vì nó nghĩa là câu trả lời KHÔNG dựa trên tài liệu nào
 */
public record ChatMessageResponse(
        UUID id,
        ChatRole role,
        String content,
        List<ChatSource> sources,
        OffsetDateTime createdAt
) {
    public static ChatMessageResponse from(ChatMessage message) {
        return new ChatMessageResponse(message.getId(), message.getRole(), message.getContent(),
                message.getSources() == null ? List.of() : message.getSources(),
                message.getCreatedAt());
    }
}
