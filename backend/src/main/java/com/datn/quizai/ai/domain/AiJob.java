package com.datn.quizai.ai.domain;

import com.datn.quizai.common.BaseEntity;
import com.datn.quizai.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/**
 * Một tác vụ AI chạy nền — bảng `ai_jobs`.
 * <p>
 * Sinh đề mất hàng chục giây, giữ HTTP request suốt thời gian đó là cách chắc chắn để gặp
 * timeout ở proxy. Nên API trả {@code 202} kèm {@code jobId}, client hỏi lại trạng thái
 * (docs/conventions.md §1 — Async).
 * <p>
 * {@code request} và {@code result} để JSON: thêm loại job mới không phải đổi schema.
 */
@Entity
@Table(name = "ai_jobs")
@Getter
@Setter
@NoArgsConstructor
public class AiJob extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AiJobType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private AiJobStatus status = AiJobStatus.PENDING;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String request;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String result;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    public AiJob(User user, AiJobType type, String request) {
        this.user = user;
        this.type = type;
        this.request = request;
    }

    public void markRunning() {
        this.status = AiJobStatus.RUNNING;
        this.startedAt = OffsetDateTime.now();
    }

    public void markSucceeded(String result) {
        this.status = AiJobStatus.SUCCEEDED;
        this.result = result;
        this.finishedAt = OffsetDateTime.now();
    }

    public void markFailed(String reason) {
        this.status = AiJobStatus.FAILED;
        this.errorMessage = reason;
        this.finishedAt = OffsetDateTime.now();
    }
}
