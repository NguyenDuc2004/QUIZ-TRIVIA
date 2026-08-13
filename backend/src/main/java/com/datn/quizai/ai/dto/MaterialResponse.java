package com.datn.quizai.ai.dto;

import com.datn.quizai.ai.domain.LearningMaterial;
import com.datn.quizai.ai.domain.MaterialSourceType;
import com.datn.quizai.ai.domain.MaterialStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Học liệu ở dạng hiển thị. Không kèm nội dung — tài liệu có thể dài hàng trăm nghìn ký tự. */
public record MaterialResponse(
        UUID id,
        String title,
        String topic,
        MaterialSourceType sourceType,
        MaterialStatus status,
        String fileUrl,
        int charCount,
        int chunkCount,
        String errorMessage,
        /** Có cho người học hỏi trợ lý AI trên tài liệu này hay không (features/08). */
        boolean shared,
        OffsetDateTime createdAt
) {
    public static MaterialResponse from(LearningMaterial material) {
        return new MaterialResponse(
                material.getId(), material.getTitle(), material.getTopic(),
                material.getSourceType(), material.getStatus(), material.getFileUrl(),
                material.getCharCount(), material.getChunkCount(),
                material.getErrorMessage(), material.isShared(), material.getCreatedAt());
    }
}
