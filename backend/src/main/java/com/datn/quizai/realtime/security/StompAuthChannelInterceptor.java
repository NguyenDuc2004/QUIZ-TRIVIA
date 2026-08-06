package com.datn.quizai.realtime.security;

import com.datn.quizai.auth.service.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Xác thực JWT ở frame STOMP {@code CONNECT} (docs/security.md §1).
 * <p>
 * Bắt tay WebSocket đi qua {@code /ws} vốn để công khai — trình duyệt không gắn được header
 * {@code Authorization} vào yêu cầu nâng cấp WebSocket, càng không gắn được khi SockJS lùi về
 * long-polling. Nên token được gửi trong header của chính frame CONNECT và kiểm tại đây:
 * kết nối nào không có token hợp lệ sẽ bị chặn <b>trước khi</b> subscribe được kênh nào.
 * <p>
 * {@code Principal} gắn ở đây chính là danh tính dùng cho mọi frame sau đó, nên client không thể
 * mạo danh người khác bằng cách nhét userId vào payload.
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(StompAuthChannelInterceptor.class);
    private static final String BEARER = "Bearer ";

    private final JwtService jwtService;

    public StompAuthChannelInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        JwtService.AuthenticatedUser user = parseToken(accessor);
        if (user == null) {
            // Ném lỗi ở frame CONNECT khiến client nhận ERROR và kết nối bị đóng
            throw new org.springframework.security.access.AccessDeniedException(
                    "Cần đăng nhập để vào phòng đấu");
        }

        accessor.setUser(new UsernamePasswordAuthenticationToken(
                user, null, List.of(new SimpleGrantedAuthority("ROLE_" + user.role().name()))));

        log.debug("WebSocket CONNECT đã xác thực: {}", user.email());
        return message;
    }

    private JwtService.AuthenticatedUser parseToken(StompHeaderAccessor accessor) {
        List<String> values = accessor.getNativeHeader("Authorization");
        if (values == null || values.isEmpty()) {
            return null;
        }

        String header = values.get(0);
        if (header == null || !header.startsWith(BEARER)) {
            return null;
        }

        try {
            return jwtService.parse(header.substring(BEARER.length()));
        } catch (RuntimeException e) {
            log.debug("Token WebSocket không hợp lệ: {}", e.getMessage());
            return null;
        }
    }
}
