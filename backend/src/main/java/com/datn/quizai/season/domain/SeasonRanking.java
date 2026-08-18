package com.datn.quizai.season.domain;

import com.datn.quizai.gamification.domain.Badge;
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

/**
 * Một dòng xếp hạng đã chốt của một mùa — bảng `season_rankings` (features/15, FR-63).
 * <p>
 * Chỉ ghi <b>sau khi mùa kết thúc</b>. Trong lúc mùa đang chạy, thứ hạng đọc từ Redis ZSET chứ không lưu ở
 * đây: ghi lại mỗi lần điểm đổi là hàng nghìn lượt ghi cho một con số sẽ đổi lại ngay sau đó.
 */
@Entity
@Table(name = "season_rankings")
@Getter
@Setter
@NoArgsConstructor
public class SeasonRanking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "final_score", nullable = false)
    private int finalScore;

    @Column(name = "final_rank", nullable = false)
    private int finalRank;

    /** Huy hiệu được trao nhờ thứ hạng này; {@code null} với người ngoài top thưởng. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reward_badge_id")
    private Badge rewardBadge;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
