package com.datn.quizai.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Cài đặt thông báo của một người (features/16, FR-70 phần bật/tắt theo loại).
 * <p>
 * Khoá chính là {@code user_id}, không phải một UUID sinh riêng — một người có đúng một bản cài đặt, nên thêm
 * một khoá thay thế chỉ mở cửa cho hai dòng cùng tồn tại.
 * <p>
 * Lưu <b>danh sách loại bị tắt</b> chứ không phải một cột boolean cho mỗi loại: thêm loại thông báo mới thì
 * không phải đụng schema. Lý do đầy đủ trong V18.
 */
@Entity
@Table(name = "notification_settings")
@Getter
@Setter
@NoArgsConstructor
public class NotificationSettings {

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    /** Mảng JSON tên các loại bị tắt, ví dụ {@code ["SRS_REMINDER"]}. Rỗng = bật tất cả. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "disabled_types", columnDefinition = "jsonb", nullable = false)
    private String disabledTypes = "[]";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public NotificationSettings(UUID userId) {
        this.userId = userId;
    }
}
