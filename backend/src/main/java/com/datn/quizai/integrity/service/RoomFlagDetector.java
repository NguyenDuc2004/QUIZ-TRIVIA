package com.datn.quizai.integrity.service;

import com.datn.quizai.integrity.domain.RoomProctoringType;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Quyết định khi nào host phòng đấu được thấy cờ đỏ về một người chơi (features/12, cảnh báo live).
 * <p>
 * Lớp thuần như {@link RiskScorer}: không Spring, không Redis, không cơ sở dữ liệu. Đây là phần có phép
 * quyết định thật — một tín hiệu sai làm một người chơi bị nghi oan giữa cuộc thi — nên nó phải kiểm được
 * bằng unit test chạy trong vài milli-giây.
 *
 * <h3>Đếm KHUÔN LẶP, không đếm số lần</h3>
 * Bài thi cá nhân dùng ngưỡng "chuyển tab 3 lần". Đưa nguyên ngưỡng đó vào phòng đấu là sai, vì phòng đấu
 * nhiễu hơn hẳn: phần lớn người chơi vào bằng điện thoại sau khi quét QR, và <b>một tin nhắn đến là một
 * {@code visibilitychange}</b>. Phòng 10 người × 10 câu thì gần như chắc chắn có người đạt 3 lần mà không
 * làm gì sai — và khi mọi người đều bị gắn cờ thì cái cờ mất nghĩa.
 * <p>
 * Nên cờ ở đây dựa trên một khuôn cụ thể: <b>rời trang rồi quay lại trong lúc câu hỏi còn sống, lặp ở
 * nhiều câu khác nhau</b>.
 *
 * <h3>Vì sao khuôn này tự loại được trường hợp vô hại</h3>
 * Client đóng số thứ tự câu <i>đang hiện trên màn hình nó</i> vào mỗi tín hiệu. Người bị gián đoạn thật —
 * nghe một cuộc gọi 30 giây — lúc quay lại thì server đã sang câu sau, và WebSocket vẫn mở nên client đã
 * nhận câu mới trong lúc ẩn: tín hiệu {@code TAB_VISIBLE} của họ mang <b>số câu khác</b>. Câu bị rời chỉ có
 * một nửa cặp, không thành khuôn.
 * <p>
 * Ngược lại người tra cứu ở tab khác phải quay lại <i>trước khi hết giờ</i> mới trả lời được — nên cả hai
 * nửa của cặp cùng nằm ở một số câu. Khuôn này không cần biết họ đi đâu; nó chỉ phân biệt <i>đi rồi về kịp
 * để trả lời</i> với <i>đi và mất câu đó</i>.
 *
 * <h3>Cờ vẫn không phải kết luận</h3>
 * Tín hiệu đến từ client nên chặn được và giả mạo được, và vẫn còn cách giải thích vô hại (đọc lại đề trên
 * một tab khác của chính ứng dụng). Vì vậy quyền duy nhất cờ này mở ra cho host là <b>nhắc riêng</b> —
 * không trừ điểm, không đuổi khỏi phòng. Xem lý do đầy đủ ở docs/features/12-anti-cheat.md.
 */
public final class RoomFlagDetector {

    /**
     * Số câu <b>khác nhau</b> có khuôn rời-rồi-về thì mới gắn cờ.
     * <p>
     * Đặt là 2 chứ không phải 3: một lần là ngẫu nhiên, hai lần đã là lặp. Đặt 3 thì một ván nhanh 3–5 câu
     * gần như không bao giờ báo, tức tính năng chỉ chạy trên giấy. Đặt 1 thì mọi người chơi điện thoại đều
     * bị gắn cờ ngay câu đầu.
     */
    public static final int NGUONG_SO_CAU = 2;

    private RoomFlagDetector() {
    }

    /**
     * @param soCauLap số câu khác nhau có khuôn rời-rồi-về — có ý nghĩa <b>cả khi chưa đủ ngưỡng</b>, vì bản
     *                 tổng kết sau ván hiện cả những người chưa bị gắn cờ
     * @param biGanCo  đã đủ khuôn để host thấy cờ đỏ chưa
     * @param lyDo     câu mô tả cho host đọc; {@code null} khi chưa gắn cờ. Một con số không kèm lý do thì
     *                 host không làm gì được với nó
     */
    public record KetQua(int soCauLap, boolean biGanCo, String lyDo) {
    }

    /**
     * Một tín hiệu đã thu được, theo đúng thứ tự thời gian.
     *
     * @param chiSoCau số thứ tự câu đang hiện lúc tín hiệu xảy ra; {@code -1} = còn ở phòng chờ
     */
    public record TinHieu(RoomProctoringType loai, int chiSoCau) {
    }

    /**
     * Một cửa vào duy nhất, dùng cho <b>cả</b> cờ trực tiếp giữa ván và bản tổng kết sau ván.
     * <p>
     * Vì vậy kết quả luôn mang {@code soCauLap} kể cả khi chưa đủ ngưỡng: bản tổng kết cần con số đó để host
     * thấy được cả những người ở mức "có tín hiệu nhưng chưa thành khuôn". Nếu hàm này chỉ trả về
     * {@code Optional} thì phần tổng kết sẽ phải tự đếm lại — tức có hai chỗ định nghĩa cùng một khái niệm,
     * và sửa ngưỡng ở một chỗ sẽ làm hai màn hình nói khác nhau.
     *
     * @param tinHieu toàn bộ tín hiệu của <b>một người chơi</b> trong phòng, theo thứ tự thời gian
     */
    public static KetQua danhGia(List<TinHieu> tinHieu) {
        int soCau = cauCoKhuonLap(tinHieu).size();
        if (soCau < NGUONG_SO_CAU) {
            return new KetQua(soCau, false, null);
        }
        return new KetQua(soCau, true,
                "Rời trang rồi quay lại trong lúc còn thời gian trả lời, ở " + soCau + " câu");
    }

    /**
     * Những số câu có <b>cả hai nửa</b> của cặp rời-rồi-về.
     * <p>
     * Tách hàm riêng và để {@code package-private} để test kiểm được chính phần này — nó là chỗ dễ sai nhất,
     * và một test đi qua {@link #danhGia} chỉ thấy "có cờ hay không" chứ không thấy câu nào bị tính.
     * <p>
     * Chỉ tính khi {@code TAB_HIDDEN} xảy ra <b>trước</b> {@code TAB_VISIBLE} ở cùng số câu. Bỏ thứ tự đi thì
     * người mở trang phòng đấu ở một tab đang ẩn sẵn (tín hiệu đầu tiên là {@code TAB_VISIBLE}) sẽ bị tính
     * oan ngay từ câu đầu.
     */
    static Set<Integer> cauCoKhuonLap(List<TinHieu> tinHieu) {
        Set<Integer> dangRoi = new LinkedHashSet<>();
        Set<Integer> ketQua = new LinkedHashSet<>();

        if (tinHieu == null) {
            return ketQua;
        }
        for (TinHieu th : tinHieu) {
            // Phòng chờ không tính: chưa có câu nào nên không có "còn thời gian trả lời" để mà tra cứu.
            if (th.chiSoCau() < 0) {
                continue;
            }
            if (th.loai() == RoomProctoringType.TAB_HIDDEN) {
                dangRoi.add(th.chiSoCau());
            } else if (dangRoi.contains(th.chiSoCau())) {
                ketQua.add(th.chiSoCau());
            }
        }
        return ketQua;
    }
}
