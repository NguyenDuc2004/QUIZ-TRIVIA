package com.datn.quizai.realtime.dto;

import java.util.UUID;

/**
 * Cờ đỏ gửi <b>riêng cho host</b> khi một người chơi lặp khuôn rời-rồi-về (features/12, cảnh báo live).
 *
 * <h3>Vì sao tuyệt đối không phát cho cả phòng</h3>
 * Phòng đấu chỉ có <b>một</b> kênh phát {@code /topic/room/{code}} và mọi người chơi đều subscribe nó. Đẩy
 * cờ lên đó là công bố tên người bị nghi cho cả phòng — làm nhục công khai dựa trên một tín hiệu client
 * chặn được và giả mạo được. Nên gói tin này chỉ đi qua {@code GameEventPublisher.toUser(..., hostId, ...)}.
 *
 * @param soCauLap số câu <b>khác nhau</b> có khuôn; client dùng nó để thay thế cờ cũ của cùng người chơi
 *                 thay vì xếp thêm một dòng mới
 * @param lyDo     câu mô tả cho host đọc. Gửi kèm lý do thay vì để client tự ghép chuỗi từ con số: mức
 *                 nghiêm khắc và cách diễn đạt đều thuộc phần quyết định, không thuộc phần hiển thị
 */
public record ProctoringFlagView(UUID playerId, String displayName, boolean guest,
                                 int soCauLap, String lyDo) {
}
