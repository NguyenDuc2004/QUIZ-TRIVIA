package com.datn.quizai.flashcard.service;

import com.datn.quizai.flashcard.domain.ReviewQuality;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Thuật toán lặp lại ngắt quãng SM-2 rút gọn (features/11, FR-40).
 * <p>
 * Tách thành lớp <b>thuần</b>, không phụ thuộc Spring hay cơ sở dữ liệu: đây là phần duy nhất của tính
 * năng có logic tính toán thật, nên nó phải kiểm được bằng unit test chạy trong vài milli-giây thay vì
 * phải dựng Testcontainers để xem một khoảng ôn có đúng hay không.
 *
 * <h3>Quy tắc</h3>
 * <ul>
 *   <li>Trả lời <b>kém</b> ({@code q < 3}): {@code repetitions = 0}, khoảng ôn về <b>1 ngày</b> — thẻ
 *       quay lại ngay ngày mai. Không về 0 ngày vì như vậy là ôn lại trong cùng phiên, và người học chỉ
 *       đang nhớ mặt chữ chứ chưa nhớ nội dung.</li>
 *   <li>Trả lời <b>tốt</b> ({@code q >= 3}): khoảng ôn đi theo 1 → 6 → {@code interval * ease}.</li>
 *   <li>{@code ease} cập nhật theo công thức SM-2 và bị chặn sàn ở <b>1.30</b>.</li>
 * </ul>
 */
public final class Sm2Scheduler {

    /** Sàn của hệ số dễ. Dưới mức này thẻ quay lại quá dày và người học không bao giờ thoát khỏi nó. */
    static final BigDecimal EASE_TOI_THIEU = new BigDecimal("1.30");

    /** Khoảng ôn lần đầu trả lời tốt. */
    static final int KHOANG_DAU = 1;

    /** Khoảng ôn lần thứ hai trả lời tốt — bước nhảy cố định của SM-2. */
    static final int KHOANG_THU_HAI = 6;

    /**
     * Chặn trên cho khoảng ôn: <b>một năm</b>.
     * <p>
     * Không có chặn này thì sau khoảng hai mươi lần trả lời "Dễ", khoảng ôn vượt quá tuổi của cả đồ án
     * và thẻ biến mất khỏi lịch mãi mãi. Một năm đủ xa để không làm phiền, và vẫn là một ngày có thật.
     */
    static final int KHOANG_TOI_DA = 365;

    private Sm2Scheduler() {
    }

    /** Kết quả tính lịch: trạng thái SM-2 mới sau một lần ôn. */
    public record LichMoi(BigDecimal easeFactor, int intervalDays, int repetitions, boolean laLanQuen) {
    }

    /**
     * Tính trạng thái mới sau một lần ôn.
     *
     * @param easeHienTai        hệ số dễ hiện tại
     * @param intervalHienTai    khoảng ôn hiện tại (ngày); 0 với thẻ chưa ôn lần nào
     * @param repetitionsHienTai số lần trả lời tốt liên tiếp hiện tại
     * @param chatLuong          mức nhớ người học tự đánh giá
     */
    public static LichMoi tinh(BigDecimal easeHienTai, int intervalHienTai, int repetitionsHienTai,
                               ReviewQuality chatLuong) {
        int q = chatLuong.diem();
        BigDecimal ease = capNhatEase(easeHienTai, q);

        if (q < 3) {
            // Trả lời kém: quên. Chỉ tính là "lần quên" nếu thẻ đã từng thuộc — sai ngay lần đầu gặp thẻ
            // là chuyện bình thường của việc học, không phải quên.
            boolean laLanQuen = repetitionsHienTai > 0;
            return new LichMoi(ease, KHOANG_DAU, 0, laLanQuen);
        }

        int repetitions = repetitionsHienTai + 1;
        int interval = switch (repetitions) {
            case 1 -> KHOANG_DAU;
            case 2 -> KHOANG_THU_HAI;
            default -> nhanTheoEase(intervalHienTai, ease);
        };
        return new LichMoi(ease, Math.min(interval, KHOANG_TOI_DA), repetitions, false);
    }

    /**
     * Công thức cập nhật hệ số dễ của SM-2:
     * {@code ease + (0.1 - (5-q) * (0.08 + (5-q) * 0.02))}.
     * <p>
     * Với {@code q = 5} hệ số tăng 0.10; {@code q = 4} giữ nguyên; {@code q = 3} giảm 0.14; càng kém thì
     * giảm càng mạnh. Chặn sàn ở {@link #EASE_TOI_THIEU} sau khi tính.
     */
    private static BigDecimal capNhatEase(BigDecimal ease, int q) {
        BigDecimal thieu = BigDecimal.valueOf(5 - q);
        BigDecimal deltaEase = BigDecimal.valueOf(0.1)
                .subtract(thieu.multiply(
                        BigDecimal.valueOf(0.08).add(thieu.multiply(BigDecimal.valueOf(0.02)))));
        BigDecimal moi = ease.add(deltaEase).setScale(2, RoundingMode.HALF_UP);
        return moi.max(EASE_TOI_THIEU);
    }

    /**
     * Khoảng ôn kế tiếp = khoảng hiện tại × ease, làm tròn lên.
     * <p>
     * Làm tròn <b>lên</b> vì làm tròn xuống có thể cho ra đúng khoảng cũ (ví dụ {@code 1 × 1.3 = 1.3 → 1}),
     * và khi đó thẻ đứng yên một chỗ mãi. Ngoài ra chặn để khoảng mới luôn lớn hơn khoảng cũ ít nhất
     * một ngày — trả lời tốt thì lịch phải giãn ra, không được đứng im.
     */
    private static int nhanTheoEase(int intervalHienTai, BigDecimal ease) {
        int nen = Math.max(intervalHienTai, KHOANG_THU_HAI);
        int moi = BigDecimal.valueOf(nen).multiply(ease).setScale(0, RoundingMode.CEILING).intValue();
        return Math.max(moi, nen + 1);
    }
}
