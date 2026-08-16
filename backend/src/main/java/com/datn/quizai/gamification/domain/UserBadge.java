package com.datn.quizai.gamification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Một huy hiệu đã trao — bảng `user_badges` (features/13, FR-50). */
@Entity
@Table(name = "user_badges")
@Getter
@Setter
@NoArgsConstructor
public class UserBadge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "badge_id", nullable = false)
    private Badge badge;

    @CreationTimestamp
    @Column(name = "earned_at", nullable = false, updatable = false)
    private OffsetDateTime earnedAt;

    public UserBadge(UUID userId, Badge badge) {
        this.userId = userId;
        this.badge = badge;
    }
}
