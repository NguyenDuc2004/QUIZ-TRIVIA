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
        OffsetDateTime createdAt,
        /**
         * Số giây còn phải chờ vì nhà cung cấp AI đang chặn hạn mức; 0 = đang chạy bình thường.
         * <p>
         * Không có con số này thì màn sinh đề chỉ có một vòng quay câm kèm dòng "thường mất 10–30
         * giây" — trong khi thực tế job đang xếp hàng chờ cả phút. Người dùng đợi ba phút rồi đi
         * tìm lỗi ở chỗ khác, dù hệ thống vẫn đang làm việc bình thường.
         */
        int aiThrottledSeconds
) {
    public static AiJobResponse from(AiJob job) {
        return from(job, 0);
    }

    public static AiJobResponse from(AiJob job, int aiThrottledSeconds) {
        boolean running = job.getStatus() == com.datn.quizai.ai.domain.AiJobStatus.PENDING
                || job.getStatus() == com.datn.quizai.ai.domain.AiJobStatus.RUNNING;
        return new AiJobResponse(
                job.getId(), job.getType(), job.getStatus(),
                job.getResult() == null ? "null" : job.getResult(),
                job.getErrorMessage(), job.getStartedAt(), job.getFinishedAt(), job.getCreatedAt(),
                // Job đã xong thì hạn mức không còn liên quan — nhắc là gây hiểu nhầm
                running ? aiThrottledSeconds : 0);
    }
}
