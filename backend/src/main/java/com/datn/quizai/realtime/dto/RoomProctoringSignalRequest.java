package com.datn.quizai.realtime.dto;

import com.datn.quizai.integrity.domain.RoomProctoringType;
import jakarta.validation.constraints.NotNull;

/**
 * Tín hiệu client gửi lên qua STOMP {@code /app/room/{code}/proctoring} (features/12, cảnh báo live).
 * <p>
 * <b>Không có trường danh tính.</b> Ai gửi thì lấy từ {@code Authentication} mà
 * {@code StompAuthChannelInterceptor} gắn lúc CONNECT — client không tự khai mình là ai được, và quan trọng
 * hơn là không khai được tín hiệu này <i>của người khác</i>.
 * <p>
 * <b>Không có mốc thời gian.</b> Server tự lấy giờ của mình, giống mọi chỗ khác trong phòng đấu: tin thời
 * gian client gửi lên thì một client sửa đổi có thể dồn mọi tín hiệu vào một câu để không thành khuôn lặp.
 */
public record RoomProctoringSignalRequest(@NotNull RoomProctoringType type) {
}
