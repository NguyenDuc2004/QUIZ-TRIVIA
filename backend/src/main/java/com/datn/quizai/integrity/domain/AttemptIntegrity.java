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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Bản tổng hợp tính toàn vẹn của một lượt thi — bảng `attempt_integrity` (features/12, FR-45).
 * <p>
 * Một lượt thi có đúng một bản (khoá duy nhất trên {@code attempt_id}), nên tính lại không tạo dòng thứ hai.
 */
@Entity
@Table(name = "attempt_integrity")
@Getter
@Setter
@NoArgsConstructor
public class AttemptIntegrity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "attempt_id", nullable = false, unique = true)
    private UUID attemptId;

    @Column(name = "risk_score", nullable = false)
    private int riskScore = 0;

    /** JSON mảng chuỗi lý do, ví dụ {@code ["Chuyển tab 4 lần","Dán nội dung 1 lần"]}. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String flags = "[]";

    /**
     * Nhận định của mô hình. {@code null} khi chưa gọi hoặc gọi thất bại — khác chuỗi rỗng, vì "AI chưa xem"
     * và "AI xem rồi không thấy gì" là hai chuyện khác nhau với người rà soát.
     */
    @Column(name = "ai_note", columnDefinition = "text")
    private String aiNote;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 10)
    private ReviewStatus reviewStatus = ReviewStatus.PENDING;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    @Column(name = "review_note", columnDefinition = "text")
    private String reviewNote;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
