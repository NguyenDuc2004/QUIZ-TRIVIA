package com.datn.quizai.realtime.domain;

import com.datn.quizai.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

    /** Null khi là khách vãng lai — xem {@link #guest}. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /**
     * Tên hiển thị chốt tại thời điểm chơi. Với khách thì đây là nguồn duy nhất; với thành viên
     * thì giữ lại tên lúc đó, sau này họ đổi tên tài khoản cũng không làm sai lịch sử ván đấu.
     */
    @Column(name = "display_name", length = 50)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private PlayerAvatar avatar;

    @Column(name = "is_guest", nullable = false)
    private boolean guest = false;

    @Column(name = "final_score", nullable = false)
    private int finalScore = 0;

    @Column(name = "joined_at", nullable = false)
    private OffsetDateTime joinedAt = OffsetDateTime.now();

    public GameRoomPlayer(User user, String displayName, PlayerAvatar avatar) {
        this.user = user;
        this.displayName = displayName;
        this.avatar = avatar;
    }

    /** Khách vãng lai: không có tài khoản, chỉ có tên và avatar chọn lúc vào phòng. */
    public static GameRoomPlayer guest(String displayName, PlayerAvatar avatar) {
        GameRoomPlayer player = new GameRoomPlayer(null, displayName, avatar);
        player.guest = true;
        return player;
    }
}
