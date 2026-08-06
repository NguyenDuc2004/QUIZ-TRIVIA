package com.datn.quizai.realtime.security;

import com.datn.quizai.auth.service.JwtService;
import com.datn.quizai.realtime.domain.RoomParticipant;
import com.datn.quizai.realtime.service.GuestSessionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Xác thực frame STOMP {@code CONNECT} (docs/security.md §1).
 * <p>
 * Chấp nhận <b>hai loại danh tính</b>:
 * <ul>
 *   <li><b>Thành viên</b> — header {@code Authorization: Bearer <JWT>}.</li>
 *   <li><b>Khách vãng lai</b> — header {@code X-Guest-Key}, khoá do
 *       {@code POST /rooms/{code}/join-as-guest} cấp và chỉ dùng được cho đúng phòng đó.</li>
 * </ul>
 * <p>
 * Bắt tay WebSocket đi qua {@code /ws} vốn để công khai — trình duyệt không gắn được header vào
 * yêu cầu nâng cấp WebSocket, càng không gắn được khi SockJS lùi về long-polling. Nên token đi
 * trong header của chính frame CONNECT và kiểm tại đây: kết nối nào không có danh tính hợp lệ
 * bị chặn <b>trước khi</b> subscribe được kênh nào.
 * <p>
 * {@code Principal} gắn ở đây là danh tính dùng cho mọi frame sau đó, nên client không thể mạo
 * danh người khác bằng cách nhét id vào payload.
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(StompAuthChannelInterceptor.class);
    private static final String BEARER = "Bearer ";
    private static final String GUEST_HEADER = "X-Guest-Key";

    private final JwtService jwtService;
    private final GuestSessionStore guestSessionStore;

    public StompAuthChannelInterceptor(JwtService jwtService, GuestSessionStore guestSessionStore) {
        this.jwtService = jwtService;
        this.guestSessionStore = guestSessionStore;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        RoomParticipant participant = resolveMember(accessor);
        String role = "ROLE_LEARNER";

        if (participant == null) {
            participant = resolveGuest(accessor);
            role = "ROLE_GUEST";
        }
        if (participant == null) {
            // Ném ở frame CONNECT khiến client nhận ERROR và kết nối bị đóng
            throw new AccessDeniedException("Cần đăng nhập hoặc vào phòng bằng mã để kết nối");
        }

        accessor.setUser(new UsernamePasswordAuthenticationToken(
                participant, null, List.of(new SimpleGrantedAuthority(role))));

        log.debug("WebSocket CONNECT: {} (khách={})", participant.playerId(), participant.guest());
        return message;
    }

    private RoomParticipant resolveMember(StompHeaderAccessor accessor) {
        String header = firstHeader(accessor, "Authorization");
        if (header == null || !header.startsWith(BEARER)) {
            return null;
        }
        try {
            return RoomParticipant.member(jwtService.parse(header.substring(BEARER.length())));
        } catch (RuntimeException e) {
            log.debug("Token WebSocket không hợp lệ: {}", e.getMessage());
            return null;
        }
    }

    private RoomParticipant resolveGuest(StompHeaderAccessor accessor) {
        return guestSessionStore.resolve(firstHeader(accessor, GUEST_HEADER))
                .map(session -> RoomParticipant.guest(session.playerId(), session.displayName()))
                .orElse(null);
    }

    private String firstHeader(StompHeaderAccessor accessor, String name) {
        List<String> values = accessor.getNativeHeader(name);
        return values == null || values.isEmpty() ? null : values.get(0);
    }
}
