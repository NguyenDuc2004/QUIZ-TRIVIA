package com.datn.quizai.integrity.service;

import com.datn.quizai.integrity.domain.ProctoringEventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Bộ tính điểm rủi ro (features/12, FR-45).
 * <p>
 * Test nhắm vào những cách bộ tính điểm có thể <b>gây oan</b>, vì đó là hậu quả thật của lớp này — điểm cao
 * làm bài của một người bị đưa ra rà soát:
 * <ol>
 *   <li><b>Bài sạch phải được 0 điểm</b> và không có cờ nào. Nếu không thì mọi bài đều bị gắn cờ và cái cờ
 *       mất nghĩa.</li>
 *   <li><b>Tín hiệu lặp nhiều lần không đẩy điểm lên trần</b> — một bài thi dài với vài chục thông báo hệ
 *       thống bật lên không được thành "gian lận".</li>
 *   <li><b>Điểm luôn nằm trong 0–100</b>, kể cả với hàng nghìn tín hiệu — cột cơ sở dữ liệu có
 *       {@code CHECK BETWEEN 0 AND 100}, vượt là vỡ khi ghi.</li>
 *   <li><b>Cờ phải nói được lý do cụ thể</b>, không chỉ một con số.</li>
 * </ol>
 */
class RiskScorerTest {

    private static RiskScorer.TinHieu th(ProctoringEventType loai) {
        return new RiskScorer.TinHieu(loai);
    }

    private static List<RiskScorer.TinHieu> lap(ProctoringEventType loai, int soLan) {
        List<RiskScorer.TinHieu> ds = new ArrayList<>();
        for (int i = 0; i < soLan; i++) {
            ds.add(th(loai));
        }
        return ds;
    }

    @Test
    @DisplayName("Bài không có tín hiệu nào: 0 điểm, không cờ")
    void cleanAttemptScoresZero() {
        assertThat(RiskScorer.tinh(List.of()).diem()).isZero();
        assertThat(RiskScorer.tinh(List.of()).co()).isEmpty();
        assertThat(RiskScorer.tinh(null).diem()).isZero();
    }

    @Test
    @DisplayName("Một lần mất focus không đủ để gắn cờ — bấm ra ngoài trang là chuyện thường")
    void singleBlurIsNotSuspicious() {
        var kq = RiskScorer.tinh(List.of(th(ProctoringEventType.WINDOW_BLUR)));

        // WINDOW_BLUR nổ cả khi người dùng chỉ bấm vào thanh tác vụ. Tính nặng thì mọi bài đều bị gắn cờ.
        assertThat(kq.bịGanCo()).isFalse();
        assertThat(kq.co()).isEmpty();
    }

    @Test
    @DisplayName("Mười lần chuyển tab KHÔNG cao gấp mười lần một lần — có giảm dần")
    void repeatedSignalsDiminish() {
        int mot = RiskScorer.tinh(lap(ProctoringEventType.TAB_HIDDEN, 1)).diem();
        int muoi = RiskScorer.tinh(lap(ProctoringEventType.TAB_HIDDEN, 10)).diem();

        assertThat(muoi).isGreaterThan(mot);
        // Cộng thẳng thì 10 × 8 = 80 điểm, tức một bài thi dài có vài chục thông báo bật lên là "gian lận".
        // Lần thứ mười chỉ xác nhận điều lần thứ hai đã nói.
        assertThat(muoi).as("giảm dần phải kìm được tổng").isLessThan(mot * 10);
    }

    @Test
    @DisplayName("Hàng nghìn tín hiệu vẫn không vượt 100 — cột cơ sở dữ liệu chặn ở 100")
    void scoreNeverExceedsHundred() {
        List<RiskScorer.TinHieu> rat_nhieu = new ArrayList<>();
        for (var loai : ProctoringEventType.values()) {
            rat_nhieu.addAll(lap(loai, 500));
        }
        // Vượt 100 là vỡ ràng buộc ck_attempt_integrity_score khi ghi — lỗi chỉ lộ ra ở tầng cơ sở dữ liệu
        assertThat(RiskScorer.tinh(rat_nhieu).diem()).isBetween(0, 100);
    }

    @Test
    @DisplayName("Dán đoạn DÀI nặng hơn dán một từ, và cờ nói rõ có đoạn dài")
    void longPasteWeighsMore() {
        var motTu = RiskScorer.tinh(List.of(new RiskScorer.TinHieu(ProctoringEventType.PASTE, 5)));
        var doanDai = RiskScorer.tinh(List.of(
                new RiskScorer.TinHieu(ProctoringEventType.PASTE, RiskScorer.DO_DAI_DAN_DANG_NGO + 50)));

        assertThat(doanDai.diem()).isGreaterThan(motTu.diem());
        assertThat(doanDai.co().get(0)).contains("đoạn dài");
        assertThat(motTu.co().get(0)).doesNotContain("đoạn dài");
    }

    @Test
    @DisplayName("Một lần dán đã đáng nói, nhưng chưa đủ để gắn cờ toàn bài")
    void singlePasteFlagsButDoesNotTripThreshold() {
        var kq = RiskScorer.tinh(List.of(new RiskScorer.TinHieu(ProctoringEventType.PASTE, 10)));

        // Có cờ để người rà soát thấy, nhưng chưa vượt ngưỡng nên bài không bị đưa vào hàng chờ. Hai mức khác
        // nhau: "có tín hiệu" và "đáng rà soát".
        assertThat(kq.co()).isNotEmpty();
        assertThat(kq.bịGanCo()).isFalse();
    }

    @Test
    @DisplayName("Nhiều loại tín hiệu cùng lúc mới vượt ngưỡng gắn cờ")
    void combinedSignalsTripThreshold() {
        List<RiskScorer.TinHieu> ds = new ArrayList<>();
        ds.addAll(lap(ProctoringEventType.TAB_HIDDEN, 4));
        ds.add(new RiskScorer.TinHieu(ProctoringEventType.PASTE, 300));
        ds.addAll(lap(ProctoringEventType.ANSWER_TOO_FAST, 2));

        var kq = RiskScorer.tinh(ds);

        assertThat(kq.bịGanCo()).isTrue();
        // Mỗi loại phải sinh một cờ riêng: người rà soát cần biết chuyện gì đã xảy ra, không chỉ điểm số
        assertThat(kq.co()).hasSize(3);
        assertThat(String.join(" | ", kq.co()))
                .contains("Chuyển tab").contains("Dán nội dung").contains("nhanh bất thường");
    }

    @Test
    @DisplayName("Thứ tự tín hiệu không đổi kết quả — cùng một bài phải ra cùng một điểm")
    void orderDoesNotMatter() {
        List<RiskScorer.TinHieu> ds = new ArrayList<>();
        ds.addAll(lap(ProctoringEventType.TAB_HIDDEN, 3));
        ds.addAll(lap(ProctoringEventType.WINDOW_BLUR, 6));
        ds.add(new RiskScorer.TinHieu(ProctoringEventType.PASTE, 200));

        int lan1 = RiskScorer.tinh(ds).diem();
        List<RiskScorer.TinHieu> daoThuTu = new ArrayList<>(ds);
        Collections.reverse(daoThuTu);

        // Cùng một bài thi mà tính hai lần ra hai điểm khác nhau thì con số không bảo vệ được trước tranh chấp
        assertThat(RiskScorer.tinh(daoThuTu).diem()).isEqualTo(lan1);
    }

    @Test
    @DisplayName("Ngưỡng gắn cờ là 60 đúng như đặc tả nêu")
    void thresholdMatchesSpec() {
        assertThat(RiskScorer.NGUONG_GAN_CO).isEqualTo(60);
    }
}
