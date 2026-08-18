package com.datn.quizai.integrity.domain;

/**
 * Kết luận của người rà soát về tính toàn vẹn một lượt thi (features/12, FR-47).
 * <p>
 * Mặc định là {@link #PENDING} và <b>chỉ người thật</b> chuyển nó sang hai trạng thái còn lại. Hệ thống không
 * bao giờ tự kết luận: tín hiệu client chặn được và giả mạo được, nên điểm rủi ro cao là lý do để xem, không
 * phải bằng chứng.
 */
public enum ReviewStatus {
    /** Chưa ai xem. Bài bị gắn cờ nằm ở đây chờ Creator hoặc Admin rà soát. */
    PENDING,
    /** Người rà soát kết luận bài hợp lệ — tín hiệu không đủ hoặc có lý do chính đáng. */
    VALID,
    /** Người rà soát kết luận bài không hợp lệ. */
    INVALID
}
