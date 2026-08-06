package com.datn.quizai.realtime.domain;

import com.datn.quizai.common.BaseEntity;
import com.datn.quizai.quiz.domain.Quiz;
import com.datn.quizai.user.domain.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Phòng đấu — bảng `game_rooms` (docs/features/04-multiplayer-realtime.md).
 * <p>
 * Entity này chỉ giữ <b>metadata và kết quả cuối</b>. Trạng thái đang chơi (câu hiện tại,
 * ai đã trả lời, điểm tạm) nằm ở Redis — xem {@code RoomStateStore} — để mỗi lượt trả lời
 * không phải ghi xuống CSDL quan hệ.
 */
@Entity
@Table(name = "game_rooms")
@Getter
@Setter
@NoArgsConstructor
public class GameRoom extends BaseEntity {

    @Column(name = "room_code", nullable = false, length = 8)
    private String roomCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RoomStatus status = RoomStatus.WAITING;

    /**
     * Host có cho khách vãng lai (chưa đăng nhập) quét QR vào phòng này không.
     * Mặc định false để giữ luật chung: chưa đăng nhập thì không vào phòng đấu.
     */
    @Column(name = "allow_guests", nullable = false)
    private boolean allowGuests = false;

    /** Thời gian mỗi câu (giây) do host chọn; null = theo cấu hình từng câu hỏi. */
    @Column(name = "seconds_per_question")
    private Integer secondsPerQuestion;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GameRoomPlayer> players = new ArrayList<>();

    public GameRoom(String roomCode, User host, Quiz quiz) {
        this.roomCode = roomCode;
        this.host = host;
        this.quiz = quiz;
    }

    public void addPlayer(GameRoomPlayer player) {
        player.setRoom(this);
        players.add(player);
    }
}
