package com.datn.quizai.ai.dto;

import com.datn.quizai.ai.domain.AiJob;
import com.datn.quizai.ai.domain.AiJobStatus;
import com.datn.quizai.ai.domain.AiJobType;
import com.fasterxml.jackson.annotation.JsonRawValue;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Trạng thái một job AI. Client hỏi lại endpoint này tới khi {@code status} kết thúc.
 *
 * @param result JSON kết quả, trả nguyên văn ({@code @JsonRawValue}) để khỏi phải escape hai lần
 */
public record AiJobResponse(
        UUID id,
        AiJobType type,
        AiJobStatus status,
        @JsonRawValue String result,
        String errorMessage,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        OffsetDateTime createdAt
) {
    public static AiJobResponse from(AiJob job) {
        return new AiJobResponse(
                job.getId(), job.getType(), job.getStatus(),
                job.getResult() == null ? "null" : job.getResult(),
                job.getErrorMessage(), job.getStartedAt(), job.getFinishedAt(), job.getCreatedAt());
    }
}
