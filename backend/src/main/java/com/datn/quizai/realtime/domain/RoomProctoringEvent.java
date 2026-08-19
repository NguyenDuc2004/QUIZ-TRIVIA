package com.datn.quizai.realtime.domain;

import com.datn.quizai.integrity.domain.RoomProctoringType;
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
 * Một tín hiệu hành vi trong phòng đấu — bảng `room_proctoring_events` (features/12, cảnh báo live).
 * <p>
 * Không có khoá ngoại tới {@code users}: một nửa người chơi phòng đấu là khách vãng lai vào bằng mã PIN và
 * không có tài khoản. {@link #playerId} là danh tính <b>trong phạm vi phòng</b> — với thành viên đó là
 * {@code users.id}, với khách là UUID sinh lúc vào phòng. Nhờ vậy phần ghi nhận không phải rẽ nhánh
 * "nếu là khách thì…", đúng như {@link RoomParticipant} đã làm cho phần chơi.
 * <p>
 * {@link #playerName} chốt tên tại thời điểm chơi vì với khách đây là nguồn duy nhất — họ không có tài khoản
 * để tra lại tên sau ván.
 */
@Entity
@Table(name = "room_proctoring_events")
@Getter
@Setter
@NoArgsConstructor
public class RoomProctoringEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "room_id", nullable = false)
    private UUID roomId;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Column(name = "player_name", length = 50)
    private String playerName;

    @Column(name = "is_guest", nullable = false)
    private boolean guest = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private RoomProctoringType eventType;

    /** Số thứ tự câu đang hiện lúc tín hiệu xảy ra; {@code -1} = còn ở phòng chờ. */
    @Column(name = "question_index", nullable = false)
    private int questionIndex = -1;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /**
     * Nhận {@code playerName} tường minh, <b>không</b> lấy từ {@link RoomParticipant}.
     * <p>
     * {@code RoomParticipant.displayName()} là null với thành viên đã đăng nhập — tên của họ nằm ở
     * {@link RoomState.PlayerState} từ lúc vào phòng nên frame STOMP không mang theo nữa. Lấy thẳng từ
     * participant thì bản tổng kết mất tên đúng những người *có* tài khoản, còn khách vẫn có tên: lỗi chỉ
     * lộ một nửa, và nửa lộ ra lại là nửa ít ai kiểm.
     */
    public RoomProctoringEvent(UUID roomId, UUID playerId, String playerName, boolean guest,
                               RoomProctoringType eventType, int questionIndex, OffsetDateTime occurredAt) {
        this.roomId = roomId;
        this.playerId = playerId;
        this.playerName = playerName;
        this.guest = guest;
        this.eventType = eventType;
        this.questionIndex = questionIndex;
        this.occurredAt = occurredAt;
    }
}
