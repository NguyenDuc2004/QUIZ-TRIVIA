package com.datn.quizai.realtime.dto;

/**
 * Lời nhắc host gửi riêng cho một người chơi (features/12, cảnh báo live).
 *
 * <h3>Vì sao chỉ có một câu, và vì sao câu đó không tố cáo</h3>
 * Nội dung là <b>mô tả điều hệ thống ghi nhận</b>, không phải lời buộc tội: "hệ thống ghi nhận bạn rời trang
 * làm bài" chứ không phải "bạn đang gian lận". Tín hiệu này vẫn có cách giải thích vô hại (một cuộc gọi đến,
 * đọc lại đề ở tab khác), nên buộc tội bằng một thông báo tự động là phạt oan người vô can — trong khi nhắc
 * thì đã đủ để người định gian lận biết mình đang bị thấy.
 * <p>
 * Không kèm số lần, không kèm điểm bị trừ: không có điểm nào bị trừ. Xem docs/features/12-anti-cheat.md.
 */
public record ProctoringWarningView(String message) {

    public static ProctoringWarningView macDinh() {
        return new ProctoringWarningView(
                "Hệ thống ghi nhận bạn rời trang phòng đấu trong lúc đang có câu hỏi. "
                        + "Chủ phòng đã được thông báo. Hãy ở lại trang cho tới khi hết ván.");
    }
}
