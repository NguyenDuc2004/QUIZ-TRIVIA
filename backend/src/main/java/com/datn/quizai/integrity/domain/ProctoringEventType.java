package com.datn.quizai.integrity.domain;

/**
 * Loại tín hiệu hành vi thu được trong chế độ thi (features/12, FR-43).
 * <p>
 * Mỗi loại kèm <b>trọng số rủi ro</b> — con số cộng vào điểm rủi ro mỗi lần xảy ra. Trọng số đặt ở đây, cạnh
 * chính định nghĩa loại sự kiện, thay vì rải trong logic tính điểm: sửa mức nghiêm khắc thì sửa một chỗ.
 * <p>
 * <b>Không loại nào ghi lại nội dung người dùng.</b> {@code PASTE} chỉ đếm số ký tự, không lưu văn bản —
 * ghi nội dung dán là thu thập dữ liệu ngoài phạm vi bài thi, điều đặc tả cấm.
 */
public enum ProctoringEventType {
    /**
     * Chuyển tab hoặc thu nhỏ cửa sổ. Trọng số vừa: một lần có thể là vô tình (thông báo hệ thống bật lên),
     * nhiều lần mới thành dấu hiệu.
     */
    TAB_HIDDEN(8),

    /**
     * Cửa sổ mất focus. Trọng số thấp hơn {@code TAB_HIDDEN} vì nó nổ cả khi người dùng chỉ bấm ra ngoài
     * trang — kể cả bấm vào thanh tác vụ. Tính nặng thì mọi bài thi đều bị gắn cờ.
     */
    WINDOW_BLUR(4),

    /** Sao chép từ đề bài. Trọng số thấp: có thể chỉ là chọn chữ để đọc cho dễ. */
    COPY(5),

    /**
     * Dán vào ô trả lời. Trọng số cao nhất trong nhóm tín hiệu client: đây là tín hiệu gần nhất với việc lấy
     * đáp án từ nơi khác. Trọng số thật còn nhân theo độ dài — xem {@code RiskScorer}.
     */
    PASTE(15),

    /** Thoát toàn màn hình khi đang ở chế độ thi nghiêm ngặt. */
    FULLSCREEN_EXIT(10),

    /**
     * Trả lời nhanh bất thường so với độ khó của câu. Server tự tính, KHÔNG nhận từ client — client tự báo
     * "tôi trả lời nhanh" thì tín hiệu này vô nghĩa.
     */
    ANSWER_TOO_FAST(12);

    private final int trongSo;

    ProctoringEventType(int trongSo) {
        this.trongSo = trongSo;
    }

    public int trongSo() {
        return trongSo;
    }
}
