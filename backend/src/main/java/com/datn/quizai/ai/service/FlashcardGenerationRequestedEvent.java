package com.datn.quizai.ai.service;

import com.datn.quizai.ai.dto.GenerateFlashcardsRequest;

import java.util.UUID;

/**
 * Phát ra khi người dùng yêu cầu sinh thẻ ghi nhớ từ học liệu (features/11, FR-38).
 * <p>
 * Xử lý chạy sau khi transaction tạo job đã commit — cùng lý do với {@link GenerationRequestedEvent}:
 * luồng nền đọc cơ sở dữ liệu sẽ chưa thấy dòng job nếu chạy trước lúc commit.
 */
public record FlashcardGenerationRequestedEvent(UUID jobId, GenerateFlashcardsRequest request,
                                                UUID ownerId) {
}
