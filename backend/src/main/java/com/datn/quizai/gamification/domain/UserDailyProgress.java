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

import java.time.OffsetDateTime;
import java.util.UUID;

/** Tiến độ thử thách ngày của một người — bảng `user_daily_progress` (features/13, FR-52). */
@Entity
@Table(name = "user_daily_progress")
@Getter
@Setter
@NoArgsConstructor
public class UserDailyProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "challenge_id", nullable = false)
    private DailyChallenge challenge;

    @Column(nullable = false)
    private int progress = 0;

    /** {@code null} = chưa hoàn thành. Có giá trị rồi thì không cộng thưởng lần nữa. */
    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    public UserDailyProgress(UUID userId, DailyChallenge challenge) {
        this.userId = userId;
        this.challenge = challenge;
    }
}
