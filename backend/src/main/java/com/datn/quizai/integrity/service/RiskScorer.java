package com.datn.quizai.integrity.service;

import com.datn.quizai.integrity.domain.ProctoringEventType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Tính điểm rủi ro và sinh cờ cho một lượt thi (features/12, FR-45).
 * <p>
 * Lớp thuần, không phụ thuộc Spring hay cơ sở dữ liệu: đây là phần có phép tính thật và là phần dễ gây tranh
 * chấp nhất (một con số quyết định bài của người khác bị rà soát hay không), nên nó phải kiểm được bằng unit
 * test chạy trong vài milli-giây.
 *
 * <h3>Điểm rủi ro KHÔNG phải xác suất gian lận</h3>
 * Nó là tổng có trọng số của các tín hiệu, chuẩn hoá về 0–100 để so sánh được giữa các bài. Một bài 80 điểm
 * không có nghĩa "80% khả năng gian lận" — nó có nghĩa "có nhiều tín hiệu đáng xem". Tín hiệu client chặn
 * được và giả mạo được, nên điểm cao là <b>lý do để người thật xem</b>, không phải kết luận.
 *
 * <h3>Vì sao có giảm dần theo số lần lặp</h3>
 * Lần chuyển tab thứ mười không đáng ngại gấp mười lần thứ nhất — nó chỉ xác nhận điều lần thứ hai đã nói.
 * Cộng thẳng thì một bài thi dài với vài chục thông báo hệ thống bật lên sẽ đạt 100 điểm mà không có gì bất
 * thường, và khi mọi bài đều bị gắn cờ thì cái cờ mất nghĩa.
 */
public final class RiskScorer {

    /** Ngưỡng gắn cờ. Từ mức này trở lên thì bài được xếp vào hàng chờ người thật rà soát. */
    public static final int NGUONG_GAN_CO = 60;

    /**
     * Số lần đầu tiên của mỗi loại được tính đủ trọng số. Từ lần thứ {@code N+1}, mỗi lần chỉ còn một phần.
     */
    private static final int SO_LAN_TINH_DU = 3;

    /** Phần trọng số còn lại cho những lần vượt {@link #SO_LAN_TINH_DU}. */
    private static final double HE_SO_GIAM_DAN = 0.3;

    /** Độ dài đoạn dán bị coi là "dài" — nhân đôi trọng số. */
    static final int DO_DAI_DAN_DANG_NGO = 120;

    private RiskScorer() {
    }

    /**
     * @param diem  0–100
     * @param co    danh sách cờ, mỗi cờ là một lý do cụ thể để người rà soát đọc. Điểm rủi ro mà không kèm lý
     *              do thì người rà soát không làm gì được với nó
     */
    public record KetQua(int diem, List<String> co) {
        public boolean bịGanCo() {
            return diem >= NGUONG_GAN_CO;
        }
    }

    /** Một tín hiệu đã thu được. {@code doDaiDan} chỉ có nghĩa với {@link ProctoringEventType#PASTE}. */
    public record TinHieu(ProctoringEventType loai, int doDaiDan) {
        public TinHieu(ProctoringEventType loai) {
            this(loai, 0);
        }
    }

    public static KetQua tinh(List<TinHieu> tinHieu) {
        if (tinHieu == null || tinHieu.isEmpty()) {
            return new KetQua(0, List.of());
        }

        Map<ProctoringEventType, Integer> soLan = new EnumMap<>(ProctoringEventType.class);
        Map<ProctoringEventType, Integer> danDai = new EnumMap<>(ProctoringEventType.class);
        double tong = 0;

        for (TinHieu th : tinHieu) {
            int lan = soLan.merge(th.loai(), 1, Integer::sum);
            double trongSo = th.loai().trongSo();

            // Đoạn dán dài đáng ngại hơn nhiều so với dán một từ: nhân đôi trọng số
            if (th.loai() == ProctoringEventType.PASTE && th.doDaiDan() >= DO_DAI_DAN_DANG_NGO) {
                trongSo *= 2;
                danDai.merge(th.loai(), 1, Integer::sum);
            }
            // Giảm dần từ lần thứ SO_LAN_TINH_DU + 1
            if (lan > SO_LAN_TINH_DU) {
                trongSo *= HE_SO_GIAM_DAN;
            }
            tong += trongSo;
        }

        int diem = (int) Math.min(100, Math.round(tong));
        return new KetQua(diem, sinhCo(soLan, danDai));
    }

    /**
     * Sinh cờ từ số lần mỗi loại.
     * <p>
     * Ngưỡng của mỗi cờ khác nhau và có lý do: một lần dán đã đáng nói, còn một lần mất focus thì không.
     */
    private static List<String> sinhCo(Map<ProctoringEventType, Integer> soLan,
                                       Map<ProctoringEventType, Integer> danDai) {
        List<String> co = new ArrayList<>();

        int chuyenTab = soLan.getOrDefault(ProctoringEventType.TAB_HIDDEN, 0);
        if (chuyenTab >= 3) {
            co.add("Chuyển tab " + chuyenTab + " lần");
        }
        int matFocus = soLan.getOrDefault(ProctoringEventType.WINDOW_BLUR, 0);
        if (matFocus >= 5) {
            co.add("Cửa sổ mất focus " + matFocus + " lần");
        }
        int dan = soLan.getOrDefault(ProctoringEventType.PASTE, 0);
        if (dan >= 1) {
            int dai = danDai.getOrDefault(ProctoringEventType.PASTE, 0);
            co.add(dai > 0
                    ? "Dán nội dung " + dan + " lần, trong đó " + dai + " lần đoạn dài"
                    : "Dán nội dung " + dan + " lần");
        }
        int thoatFullscreen = soLan.getOrDefault(ProctoringEventType.FULLSCREEN_EXIT, 0);
        if (thoatFullscreen >= 1) {
            co.add("Thoát toàn màn hình " + thoatFullscreen + " lần");
        }
        int quaNhanh = soLan.getOrDefault(ProctoringEventType.ANSWER_TOO_FAST, 0);
        if (quaNhanh >= 2) {
            co.add(quaNhanh + " câu trả lời nhanh bất thường");
        }
        return co;
    }
}
