package com.datn.quizai.attempt.service;

import com.datn.quizai.quiz.domain.Difficulty;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Chọn câu hỏi kế tiếp theo chuỗi đúng/sai của người học (features/03, FR-32).
 *
 * <h3>Đổi THỨ TỰ, không đổi BỘ ĐỀ — và đây là quyết định trung tâm</h3>
 * Cách hiểu thông thường của "adaptive difficulty" là chọn ra một <i>tập câu hỏi khác nhau</i> cho từng
 * người. Không làm vậy, vì bán kính ảnh hưởng của nó lớn hơn nhiều so với giá trị nó mang lại:
 * <ul>
 *   <li>Hai người làm hai bộ câu khác nhau thì <b>điểm không so được với nhau</b> — và bảng xếp hạng theo
 *       quiz (FR-19), bảng theo dõi lớp (FR-57), thống kê quiz (features/09) đều đang dựa trên giả định
 *       ngược lại.</li>
 *   <li>Đề được <b>chốt ngay lúc bắt đầu</b> ({@code attempt_answers} sao lại toàn bộ câu hỏi kèm điểm tối
 *       đa) để chủ quiz sửa đề giữa chừng không làm hỏng bài đang làm. Chọn câu động là phá bất biến đó.</li>
 * </ul>
 * Nên bộ đề giữ nguyên, <b>thứ tự thì thích ứng</b>: sai liên tiếp thì gặp câu dễ hơn, đúng liên tiếp thì
 * gặp câu khó hơn. Mọi câu vẫn được hỏi, nên điểm vẫn so được và mọi tính năng dựa trên điểm vẫn đúng.
 *
 * <h3>Chỉ áp cho chế độ LUYỆN TẬP</h3>
 * Thi là để <b>đo</b>: mọi người phải làm cùng một đề theo cùng một thứ tự, nếu không thì thứ tự trở thành
 * một biến số ảnh hưởng tới điểm mà không ai kiểm soát. Luyện tập là để <b>học</b>, và ở đó thích ứng có
 * ích thật: người sai ba câu liền cần một câu dễ để lấy lại đà, không phải câu khó thứ tư.
 *
 * <h3>Lớp thuần, không Spring</h3>
 * Giống {@code RiskScorer} và {@code RoomFlagDetector}: đây là phần có phép quyết định, nên nó phải kiểm
 * được bằng unit test chạy trong vài milli-giây.
 */
public final class AdaptiveNextPicker {

    /**
     * Số câu liên tiếp cùng kết quả thì mới đổi hướng.
     * <p>
     * Đặt 2 chứ không phải 1: một câu sai có thể là bấm nhầm hoặc một câu lắt léo, và đổi hướng ngay lập
     * tức làm độ khó nhảy lên xuống theo từng câu — người học cảm thấy đề "loạn" chứ không thấy nó thích
     * ứng. Hai câu liền đã là một xu hướng.
     */
    static final int CHUOI_DOI_HUONG = 2;

    private AdaptiveNextPicker() {
    }

    /**
     * Một câu trong đề, rút gọn còn đúng thứ thuật toán cần.
     *
     * @param traLoiLuc thời điểm trả lời; null khi chưa làm. <b>Bắt buộc phải có</b> — xem
     *                  {@link #huongTheoChuoi} về việc vì sao thứ tự ĐỀ không dùng được
     */
    public record Cau(UUID questionId, int thuTu, Difficulty doKho, Boolean ketQua,
                      OffsetDateTime traLoiLuc) {
        /** Chưa trả lời thì {@code ketQua} là null. */
        boolean chuaLam() {
            return ketQua == null;
        }
    }

    /**
     * @param cacCau toàn bộ câu trong đề, theo thứ tự gốc; {@code ketQua} null = chưa làm
     * @return câu nên hỏi tiếp; {@link Optional#empty()} khi đã làm hết
     */
    public static Optional<Cau> chonTiepTheo(List<Cau> cacCau) {
        if (cacCau == null || cacCau.isEmpty()) {
            return Optional.empty();
        }
        List<Cau> conLai = cacCau.stream().filter(Cau::chuaLam).toList();
        if (conLai.isEmpty()) {
            return Optional.empty();
        }

        Huong huong = huongTheoChuoi(cacCau);

        return switch (huong) {
            // Sai liên tiếp → câu DỄ nhất còn lại, để người học lấy lại đà
            case DE_HON -> Optional.of(conLai.stream()
                    .min(Comparator.comparingInt(c -> thang(c.doKho())))
                    .orElseThrow());
            // Đúng liên tiếp → câu KHÓ nhất còn lại, để không phí thời gian với thứ họ đã nắm
            case KHO_HON -> Optional.of(conLai.stream()
                    .max(Comparator.comparingInt(c -> thang(c.doKho())))
                    .orElseThrow());
            // Chưa thành xu hướng → giữ nguyên thứ tự gốc của đề
            case GIU_NGUYEN -> Optional.of(conLai.getFirst());
        };
    }

    enum Huong { DE_HON, KHO_HON, GIU_NGUYEN }

    /**
     * Đọc chuỗi kết quả gần nhất theo <b>thứ tự người học đã làm</b>, không theo thứ tự đề.
     * <p>
     * Package-private để test kiểm thẳng được phần này — nó là chỗ dễ sai nhất, và một test đi qua
     * {@link #chonTiepTheo} chỉ thấy "chọn câu nào" chứ không thấy vì sao.
     */
    static Huong huongTheoChuoi(List<Cau> cacCau) {
        // Sắp theo THỜI ĐIỂM TRẢ LỜI, không theo thứ tự đề.
        //
        // Chính thuật toán này đã đổi thứ tự ở các bước trước, nên hai thứ tự đó khác nhau. Lọc danh sách
        // đề rồi lấy hai phần tử cuối sẽ ra hai câu có SỐ THỨ TỰ lớn nhất, chứ không phải hai câu người
        // học VỪA làm — và chuỗi đọc ra sai hoàn toàn. Đây là chỗ dễ nhầm nhất của cả lớp.
        List<Cau> daLam = cacCau.stream()
                .filter(c -> !c.chuaLam())
                .sorted(Comparator.comparing(Cau::traLoiLuc,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .toList();
        if (daLam.size() < CHUOI_DOI_HUONG) {
            return Huong.GIU_NGUYEN;
        }

        List<Cau> ganNhat = daLam.subList(daLam.size() - CHUOI_DOI_HUONG, daLam.size());
        boolean toanDung = ganNhat.stream().allMatch(c -> Boolean.TRUE.equals(c.ketQua()));
        boolean toanSai = ganNhat.stream().allMatch(c -> Boolean.FALSE.equals(c.ketQua()));

        if (toanDung) {
            return Huong.KHO_HON;
        }
        return toanSai ? Huong.DE_HON : Huong.GIU_NGUYEN;
    }

    /**
     * Thang độ khó để so sánh.
     * <p>
     * {@code null} coi như MEDIUM: câu chưa đặt độ khó không được rơi xuống đáy hay lên đỉnh thang, vì
     * "không biết" khác "dễ" và cũng khác "khó".
     */
    private static int thang(Difficulty doKho) {
        if (doKho == null) {
            return 1;
        }
        return switch (doKho) {
            case EASY -> 0;
            case MEDIUM -> 1;
            case HARD -> 2;
        };
    }
}
