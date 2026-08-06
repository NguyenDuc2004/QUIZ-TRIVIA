package com.datn.quizai.realtime.domain;

import com.datn.quizai.user.domain.User;
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

/**
 * Một người chơi trong phòng — bảng `game_room_players`.
 * <p>
 * {@code finalScore} chỉ được ghi khi ván kết thúc; trong lúc chơi điểm nằm ở Redis.
 */
@Entity
@Table(name = "game_room_players")
@Getter
@Setter
@NoArgsConstructor
public class GameRoomPlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private GameRoom room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "final_score", nullable = false)
    private int finalScore = 0;

    @Column(name = "joined_at", nullable = false)
    private OffsetDateTime joinedAt = OffsetDateTime.now();

    public GameRoomPlayer(User user) {
        this.user = user;
    }
}
