package com.datn.quizai.realtime.controller;

import com.datn.quizai.common.exception.BusinessException;
import com.datn.quizai.realtime.domain.RoomParticipant;
import com.datn.quizai.realtime.dto.SubmitRoomAnswerRequest;
import com.datn.quizai.realtime.service.RoomService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.util.Map;

/**
 * Điều khiển ván đấu qua STOMP — docs/api.md §5.2.
 * <p>
 * Danh tính người gửi lấy từ {@code Authentication} mà {@code StompAuthChannelInterceptor} gắn
 * lúc CONNECT, <b>không</b> lấy từ payload — client không tự khai mình là ai được. Danh tính đó
 * là {@link RoomParticipant}, che đi việc người gửi là thành viên hay khách vãng lai.
 */
@Controller
public class RoomStompController {

    private static final Logger log = LoggerFactory.getLogger(RoomStompController.class);

    private final RoomService roomService;

    public RoomStompController(RoomService roomService) {
        this.roomService = roomService;
    }

    @MessageMapping("/room/{roomCode}/start")
    public void start(@DestinationVariable String roomCode, Authentication authentication) {
        roomService.start(roomCode, participant(authentication));
    }

    @MessageMapping("/room/{roomCode}/answer")
    public void answer(@DestinationVariable String roomCode,
                       @Payload SubmitRoomAnswerRequest request,
                       Authentication authentication) {
        roomService.answer(roomCode, request, participant(authentication));
    }

    @MessageMapping("/room/{roomCode}/next")
    public void next(@DestinationVariable String roomCode, Authentication authentication) {
        roomService.next(roomCode, participant(authentication));
    }

    @MessageMapping("/room/{roomCode}/ready")
    public void ready(@DestinationVariable String roomCode,
                      @Payload Map<String, Object> payload,
                      Authentication authentication) {
        boolean ready = !Boolean.FALSE.equals(payload.get("ready"));
        roomService.setReady(roomCode, participant(authentication), ready);
    }

    @MessageMapping("/room/{roomCode}/avatar")
    public void avatar(@DestinationVariable String roomCode,
                       @Payload Map<String, Object> payload,
                       Authentication authentication) {
        Object avatar = payload.get("avatar");
        roomService.setAvatar(roomCode, participant(authentication),
                avatar == null ? null : avatar.toString());
    }

    /**
     * Lỗi nghiệp vụ trong ván đấu (hết giờ, đã trả lời rồi, không phải host…) được gửi riêng cho
     * người gây ra, không phát cho cả phòng. Không có {@code @RestControllerAdvice} nào bắt được
     * lỗi ở kênh STOMP nên phải xử lý tại đây.
     */
    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public Map<String, Object> handleError(Exception exception) {
        if (exception instanceof BusinessException business) {
            return Map.of("status", business.getStatus().value(), "message", business.getMessage());
        }
        log.error("Lỗi không lường trước trong phòng đấu", exception);
        return Map.of("status", 500, "message", "Đã có lỗi xảy ra trong ván đấu");
    }

    private RoomParticipant participant(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof RoomParticipant participant)) {
            throw BusinessException.unauthorized("Phiên WebSocket chưa được xác thực");
        }
        return participant;
    }
}
