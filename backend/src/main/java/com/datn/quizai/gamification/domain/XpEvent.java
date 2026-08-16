package com.datn.quizai.gamification.domain;

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

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Một lần cộng XP — bảng `xp_events` (features/13).
 * <p>
 * Sổ ghi này tồn tại để bảo đảm <b>idempotent</b>: cộng thẳng vào {@code user_stats.total_xp} thì không có
 * cách nào biết một hành động đã được tính chưa, và một lần retry là một lần cộng đôi. Ràng buộc
 * {@code UNIQUE (user_id, source_type, source_key)} để cơ sở dữ liệu chặn — kiểm trong Java thua cuộc khi
 * hai luồng chạy song song.
 */
@Entity
@Table(name = "xp_events")
@Getter
@Setter
@NoArgsConstructor
public class XpEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private XpSource sourceType;

    @Column(name = "source_key", nullable = false, length = 120)
    private String sourceKey;

    @Column(nullable = false)
    private int xp;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public XpEvent(UUID userId, XpSource sourceType, String sourceKey, int xp) {
        this.userId = userId;
        this.sourceType = sourceType;
        this.sourceKey = sourceKey;
        this.xp = xp;
    }
}
