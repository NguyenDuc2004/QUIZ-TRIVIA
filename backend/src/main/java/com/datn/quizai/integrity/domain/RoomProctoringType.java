package com.datn.quizai.integrity.domain;

/**
 * Tín hiệu hành vi thu được trong <b>phòng đấu real-time</b> (features/12, phần cảnh báo live).
 *
 * <h3>Vì sao là enum riêng, không dùng lại {@link ProctoringEventType}</h3>
 * Hai lý do, cái thứ hai quan trọng hơn:
 * <ol>
 *   <li>{@code ProctoringEventType} mang <b>trọng số rủi ro</b> để cộng thành một con số 0–100. Phòng đấu
 *       không tính điểm rủi ro (xem {@code RoomFlagDetector}), nên trọng số ở đây là một trường vô nghĩa —
 *       và một trường vô nghĩa sẽ có người dùng nó.</li>
 *   <li>Phòng đấu cần {@link #TAB_VISIBLE}, thứ mà <b>bài thi cố ý không thu</b>: ở bài thi, "quay lại" không
 *       phải tín hiệu và ghi cả hai chiều làm số lần chuyển tab bị đếm gấp đôi. Ở phòng đấu thì *quay lại kịp
 *       giờ* mới chính là dấu hiệu — nên cùng một cặp sự kiện lại có ý nghĩa trái ngược ở hai chỗ.</li>
 * </ol>
 * Gộp hai enum sẽ buộc một trong hai bên phải mang khái niệm của bên kia.
 */
public enum RoomProctoringType {
    /** Người chơi rời trang phòng đấu: chuyển tab, thu nhỏ cửa sổ, hoặc tắt màn hình. */
    TAB_HIDDEN,

    /** Người chơi quay lại trang phòng đấu. */
    TAB_VISIBLE
}
