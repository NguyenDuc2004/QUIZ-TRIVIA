package com.datn.quizai.flashcard.service;

import com.datn.quizai.flashcard.domain.ReviewQuality;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Thuật toán SM-2 (features/11, FR-40).
 * <p>
 * Kiểm những cách hỏng có thật của một bộ lập lịch ôn tập, không kiểm lại công thức bằng cách viết lại
 * công thức:
 * <ol>
 *   <li><b>Thẻ đứng yên một chỗ</b> — trả lời tốt mà khoảng ôn không giãn ra thì thẻ ôn mãi không xong.</li>
 *   <li><b>Thẻ biến mất</b> — khoảng ôn vượt quá đời của hệ thống thì thẻ không bao giờ quay lại.</li>
 *   <li><b>Trả lời kém mà không quay lại sớm</b> — mất luôn tác dụng của việc lặp lại ngắt quãng.</li>
 *   <li><b>Hệ số dễ trôi xuống dưới sàn</b> — sàn 1.30 là của thuật toán, xuyên qua nó thì lịch ôn dày
 *       tới mức người học không thoát khỏi thẻ.</li>
 * </ol>
 */
class Sm2SchedulerTest {

    private static final BigDecimal EASE_MAC_DINH = new BigDecimal("2.50");

    @Test
    @DisplayName("Chuỗi trả lời tốt: khoảng ôn đi 1 → 6 → giãn dần, KHÔNG bao giờ đứng yên")
    void goodAnswersAlwaysGrowTheInterval() {
        BigDecimal ease = EASE_MAC_DINH;
        int interval = 0;
        int reps = 0;

        int truoc = -1;
        for (int lan = 1; lan <= 10; lan++) {
            var moi = Sm2Scheduler.tinh(ease, interval, reps, ReviewQuality.GOOD);
            ease = moi.easeFactor();
            interval = moi.intervalDays();
            reps = moi.repetitions();

            if (lan == 1) {
                assertThat(interval).as("lần đầu trả lời tốt").isEqualTo(1);
            } else if (lan == 2) {
                assertThat(interval).as("lần thứ hai — bước nhảy cố định của SM-2").isEqualTo(6);
            } else {
                // Đây là phép kiểm quan trọng nhất: nhân với ease rồi làm tròn có thể cho ra đúng khoảng
                // cũ (1 × 1.3 = 1.3 → 1), và khi đó thẻ đứng yên một chỗ mãi mãi.
                assertThat(interval).as("lần %d phải giãn hơn lần trước", lan).isGreaterThan(truoc);
            }
            truoc = interval;
            assertThat(reps).isEqualTo(lan);
        }
    }

    @Test
    @DisplayName("Khoảng ôn bị chặn ở 1 năm — thẻ không được biến mất khỏi lịch vĩnh viễn")
    void intervalIsCappedAtOneYear() {
        BigDecimal ease = EASE_MAC_DINH;
        int interval = 0;
        int reps = 0;

        // Trả lời "Dễ" ba mươi lần: không có chặn trên thì khoảng ôn vượt xa tuổi của cả hệ thống
        for (int i = 0; i < 30; i++) {
            var moi = Sm2Scheduler.tinh(ease, interval, reps, ReviewQuality.EASY);
            ease = moi.easeFactor();
            interval = moi.intervalDays();
            reps = moi.repetitions();
        }
        assertThat(interval).isEqualTo(365);
    }

    @Test
    @DisplayName("Trả lời kém: khoảng ôn về 1 ngày và chuỗi đúng liên tiếp bị reset")
    void poorAnswerResetsTheStreak() {
        // Dựng một thẻ đã thuộc khá lâu
        var sauMotLan = Sm2Scheduler.tinh(EASE_MAC_DINH, 0, 0, ReviewQuality.GOOD);
        var sauHaiLan = Sm2Scheduler.tinh(sauMotLan.easeFactor(), sauMotLan.intervalDays(),
                sauMotLan.repetitions(), ReviewQuality.GOOD);
        var sauBaLan = Sm2Scheduler.tinh(sauHaiLan.easeFactor(), sauHaiLan.intervalDays(),
                sauHaiLan.repetitions(), ReviewQuality.GOOD);
        assertThat(sauBaLan.intervalDays()).isGreaterThan(6);

        var quen = Sm2Scheduler.tinh(sauBaLan.easeFactor(), sauBaLan.intervalDays(),
                sauBaLan.repetitions(), ReviewQuality.AGAIN);

        assertThat(quen.intervalDays()).as("thẻ quay lại ngày mai").isEqualTo(1);
        assertThat(quen.repetitions()).isZero();
        assertThat(quen.laLanQuen()).as("thẻ đã từng thuộc rồi mới sai — đó là quên").isTrue();
        assertThat(quen.easeFactor()).as("hệ số dễ giảm").isLessThan(sauBaLan.easeFactor());
    }

    @Test
    @DisplayName("HARD tính là chưa nhớ: cũng đưa thẻ về 1 ngày, không giãn lịch")
    void hardCountsAsNotRemembered() {
        var moi = Sm2Scheduler.tinh(EASE_MAC_DINH, 6, 2, ReviewQuality.HARD);

        // Ranh giới của SM-2 nằm giữa 2 và 3. HARD = 2 nên nằm bên "chưa nhớ" — chỗ này dễ hiểu sai
        // thành "nhớ nhưng khó" rồi cho giãn lịch, và thẻ khó nhất lại bị ôn thưa nhất.
        assertThat(moi.intervalDays()).isEqualTo(1);
        assertThat(moi.repetitions()).isZero();
    }

    @Test
    @DisplayName("Sai liên tục không đẩy hệ số dễ xuống dưới sàn 1.30")
    void easeNeverGoesBelowFloor() {
        BigDecimal ease = EASE_MAC_DINH;
        for (int i = 0; i < 50; i++) {
            ease = Sm2Scheduler.tinh(ease, 1, 0, ReviewQuality.AGAIN).easeFactor();
        }
        assertThat(ease).isEqualByComparingTo(new BigDecimal("1.30"));
    }

    @Test
    @DisplayName("Sai ngay lần đầu gặp thẻ KHÔNG tính là quên")
    void firstTimeFailureIsNotALapse() {
        var moi = Sm2Scheduler.tinh(EASE_MAC_DINH, 0, 0, ReviewQuality.AGAIN);

        // Không nhớ một thẻ chưa từng học là chuyện bình thường của việc học. Tính nó thành "quên" thì
        // số liệu "thẻ hay quên" đầy thẻ mới và mất tác dụng chỉ ra thẻ thật sự khó.
        assertThat(moi.laLanQuen()).isFalse();
        assertThat(moi.intervalDays()).isEqualTo(1);
    }

    @Test
    @DisplayName("EASY tăng hệ số dễ, GOOD giữ nguyên — đúng công thức SM-2")
    void easeMovesAccordingToQuality() {
        assertThat(Sm2Scheduler.tinh(EASE_MAC_DINH, 6, 2, ReviewQuality.EASY).easeFactor())
                .isEqualByComparingTo(new BigDecimal("2.60"));
        assertThat(Sm2Scheduler.tinh(EASE_MAC_DINH, 6, 2, ReviewQuality.GOOD).easeFactor())
                .as("q=4 giữ nguyên; GOOD là q=3 nên giảm nhẹ")
                .isEqualByComparingTo(new BigDecimal("2.36"));
    }
}
