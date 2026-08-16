package com.datn.quizai.gamification.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Một huy hiệu, kèm trạng thái mở khoá của người đang gọi (features/13, FR-50).
 *
 * @param earnedAt {@code null} = chưa mở khoá. Trả cả huy hiệu chưa đạt chứ không chỉ huy hiệu đã đạt: danh
 *                 sách đầy đủ cho người học thấy còn gì để hướng tới, còn danh sách chỉ có cái đã đạt thì
 *                 không tạo được động lực nào
 */
public record BadgeResponse(
        UUID id,
        String code,
        String name,
        String description,
        String icon,
        OffsetDateTime earnedAt
) {
    public boolean daMoKhoa() {
        return earnedAt != null;
    }
}
