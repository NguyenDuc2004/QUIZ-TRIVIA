package com.datn.quizai.gamification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Chỉ số trò chơi hoá của một người dùng — bảng `user_stats` (features/13).
 * <p>
 * {@code userId} là khoá chính, không có cột {@code id} riêng: một người có đúng một dòng chỉ số, nên thêm
 * một khoá thay thế chỉ tạo ra khả năng tồn tại hai dòng cho cùng một người.
 * <p>
 * Không kế thừa {@code BaseEntity} vì lớp đó áp khoá chính UUID tự sinh.
 */
@Entity
@Table(name = "user_stats")
@Getter
@Setter
@NoArgsConstructor
public class UserStats {

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "total_xp", nullable = false)
    private int totalXp = 0;

    @Column(nullable = false)
    private int level = 1;

    @Column(name = "current_streak", nullable = false)
    private int currentStreak = 0;

    /** Chuỗi dài nhất từng đạt. Không bao giờ giảm — đó là thành tích, không phải trạng thái. */
    @Column(name = "longest_streak", nullable = false)
    private int longestStreak = 0;

    @Column(name = "last_active_date")
    private LocalDate lastActiveDate;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public UserStats(UUID userId) {
        this.userId = userId;
    }
}
