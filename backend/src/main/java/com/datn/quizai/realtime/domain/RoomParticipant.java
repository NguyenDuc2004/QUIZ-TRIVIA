package com.datn.quizai.realtime.domain;

import com.datn.quizai.auth.service.JwtService;
import org.springframework.security.core.AuthenticatedPrincipal;

import java.util.UUID;

/**
 * Một người đang ở trong phòng, bất kể là thành viên hay khách vãng lai.
 * <p>
 * Nhờ lớp này, {@code RoomService} không phải rẽ nhánh "nếu là khách thì…" ở mọi chỗ: tính điểm,
 * xếp hạng, kiểm đã trả lời chưa đều chỉ cần {@code playerId}.
 * <p>
 * Cài {@link AuthenticatedPrincipal} để {@code Authentication.getName()} trả về id — đó là khoá
 * mà {@code convertAndSendToUser} dùng để tìm đúng phiên WebSocket khi gửi tin nhắn riêng.
 *
 * @param displayName có thể null với thành viên: tên của họ đã nằm trong trạng thái phòng từ lúc
 *                    vào, frame STOMP không cần mang theo nữa
 */
public record RoomParticipant(UUID playerId, String displayName, boolean guest)
        implements AuthenticatedPrincipal {

    @Override
    public String getName() {
        return playerId.toString();
    }

    public static RoomParticipant member(JwtService.AuthenticatedUser user) {
        return new RoomParticipant(user.id(), null, false);
    }

    public static RoomParticipant member(UUID userId, String displayName) {
        return new RoomParticipant(userId, displayName, false);
    }

    public static RoomParticipant guest(UUID playerId, String displayName) {
        return new RoomParticipant(playerId, displayName, true);
    }
}
