package com.datn.quizai.notification.domain;

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
 * Một thông báo gửi cho một người (features/16, FR-65).
 * <p>
 * Không kế thừa {@code BaseEntity} vì bảng cố ý <b>không có</b> {@code updated_at}: thông báo chỉ thay đổi
 * đúng một lần trong đời, lúc được đọc, và nếu cần biết *khi nào* đọc thì thứ đúng để thêm là {@code readAt}
 * chứ không phải một mốc cập nhật chung.
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private NotificationType type;

    @Column(nullable = false, length = 200, updatable = false)
    private String title;

    @Column(updatable = false)
    private String body;

    /**
     * Dữ liệu cho nút bấm của giao diện, ví dụ {@code {"deckId":"..."}}.
     * <p>
     * {@code @JdbcTypeCode(SqlTypes.JSON)} là <b>bắt buộc</b>, không phải tuỳ chọn: thiếu nó thì Hibernate gửi
     * String sang cột {@code jsonb} và PostgreSQL từ chối. {@code columnDefinition} không thay được — nó chỉ
     * ảnh hưởng lúc sinh schema, mà schema ở đây do Flyway tạo.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", updatable = false)
    private String data;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    /** Khoá chống trùng; null khi không cần. Xem javadoc trong V18. */
    @Column(name = "dedupe_key", length = 100, updatable = false)
    private String dedupeKey;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public Notification(UUID userId, NotificationType type, String title, String body,
                        String data, String dedupeKey) {
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.body = body;
        this.data = data;
        this.dedupeKey = dedupeKey;
    }
}
