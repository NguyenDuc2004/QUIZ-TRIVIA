package com.datn.quizai.integrity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Một tín hiệu hành vi trong lượt thi — bảng `proctoring_events` (features/12, FR-43).
 * <p>
 * {@code detail} <b>không chứa nội dung người dùng</b>: với {@code PASTE} chỉ có độ dài, với
 * {@code ANSWER_TOO_FAST} chỉ có số giây. Ghi nội dung dán là thu thập dữ liệu ngoài phạm vi bài thi.
 */
@Entity
@Table(name = "proctoring_events")
@Getter
@Setter
@NoArgsConstructor
public class ProctoringEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "attempt_id", nullable = false)
    private UUID attemptId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private ProctoringEventType eventType;

    /** JSON, ví dụ {@code {"length":240}}. Cần @JdbcTypeCode để Hibernate gửi đúng kiểu jsonb. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String detail;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
