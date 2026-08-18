package com.datn.quizai.season.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Một mùa đã kết thúc kèm thành tích của người gọi (features/15, FR-63).
 *
 * @param finalRank  {@code null} khi người này không có mặt trong bảng lưu trữ của mùa đó — họ không kiếm
 *                   điểm nào, hoặc mùa đó kết thúc trước khi họ đăng ký
 * @param tenHuyHieu tên huy hiệu nhận được nhờ thứ hạng; {@code null} nếu ngoài top thưởng
 */
public record SeasonHistoryItem(
        UUID seasonId,
        String tenMua,
        OffsetDateTime ketThuc,
        Integer finalRank,
        Integer finalScore,
        String tenHuyHieu,
        String iconHuyHieu
) {
}
