package com.datn.quizai.realtime.dto;

import java.util.UUID;

/**
 * Một dòng trong bản tổng kết chống gian lận host xem <b>sau ván</b> (features/12, cảnh báo live).
 *
 * <h3>Vì sao có cả người chưa bị gắn cờ</h3>
 * Bản này liệt kê mọi người <i>có tín hiệu</i>, không chỉ người vượt ngưỡng. Chỉ hiện người bị gắn cờ thì
 * host không phân biệt được "không ai làm gì" với "hệ thống không thu được gì" — hai tình huống rất khác nhau
 * mà lại cho ra cùng một danh sách trống.
 *
 * @param soLanRoiTrang tổng số lần rời trang. Con số này <b>không phải</b> căn cứ gắn cờ — nó chỉ để host
 *                      thấy độ ồn của từng người. Căn cứ là {@link #soCauLap}
 * @param soCauLap      số câu khác nhau có khuôn rời-rồi-về; đây mới là thứ quyết định
 * @param biGanCo       đã vượt ngưỡng khuôn lặp chưa
 */
public record RoomProctoringPlayerSummary(UUID playerId, String displayName, boolean guest,
                                          int soLanRoiTrang, int soCauLap, boolean biGanCo) {
}
