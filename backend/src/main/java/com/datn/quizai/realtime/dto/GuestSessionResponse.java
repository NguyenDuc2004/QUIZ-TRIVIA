package com.datn.quizai.realtime.dto;

import java.util.UUID;

/**
 * Trả về sau khi khách vào phòng.
 *
 * @param guestKey khoá phiên client phải giữ lại: gửi kèm ở header {@code X-Guest-Key} khi nối
 *                 WebSocket. Chỉ dùng được cho đúng phòng này và tự hết hạn.
 * @param playerId id của khách trong phòng — client cần để biết thẻ nào trong phòng chờ là mình.
 *                 Không suy ra từ tên được: hai khách hoàn toàn có thể trùng biệt danh.
 */
public record GuestSessionResponse(String guestKey, UUID playerId, RoomView room) {
}
