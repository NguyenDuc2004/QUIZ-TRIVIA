package com.datn.quizai.ai.service;

import com.datn.quizai.ai.dto.GenerateQuestionsRequest;

import java.util.UUID;

/**
 * Phát ra khi Creator yêu cầu sinh đề. Xử lý chạy sau khi transaction tạo job đã commit —
 * cùng lý do với {@link MaterialCreatedEvent}.
 */
public record GenerationRequestedEvent(UUID jobId, GenerateQuestionsRequest request, UUID ownerId) {
}
