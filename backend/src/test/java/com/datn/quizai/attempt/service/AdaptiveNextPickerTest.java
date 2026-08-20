package com.datn.quizai.attempt.service;

import com.datn.quizai.quiz.domain.Difficulty;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Thứ tự thích ứng theo chuỗi đúng/sai (features/03, FR-32).
 *
 * <h3>Bất biến quan trọng nhất: BỘ ĐỀ KHÔNG ĐỔI</h3>
 * Lớp này chỉ chọn <b>thứ tự</b>. Nếu một ngày nó bắt đầu bỏ bớt câu hoặc thêm câu, thì hai người học sẽ
 * có điểm không so được với nhau — và bảng xếp hạng theo quiz, bảng theo dõi lớp, thống kê quiz đều đang
 * dựa trên giả định ngược lại. Test cuối cùng chốt đúng điều đó.
 *
 * <h3>Chỗ dễ sai nhất: thứ tự LÀM khác thứ tự ĐỀ</h3>
 * Chính thuật toán này đã đổi thứ tự ở các bước trước, nên "hai câu gần nhất" phải đọc theo <b>thời điểm
 * trả lời</b>. Đọc theo thứ tự đề thì ra hai câu có số thứ tự lớn nhất — không phải hai câu người học vừa
 * làm. Có test riêng cho đúng cái bẫy này.
 */
class AdaptiveNextPickerTest {

    private static final OffsetDateTime MOC = OffsetDateTime.parse("2026-08-20T10:00:00Z");

    @Test
    @DisplayName("Chưa làm câu nào: giữ nguyên thứ tự đề")
    void shouldKeepOriginalOrderAtStart() {
        var de = List.of(cau(0, Difficulty.HARD, null, 0), cau(1, Difficulty.EASY, null, 0));

        assertThat(chon(de)).isEqualTo(de.getFirst().questionId());
    }

    @Test
    @DisplayName("Mới sai MỘT câu: chưa đổi hướng — một câu sai có thể là bấm nhầm")
    void shouldNotSwitchAfterSingleWrong() {
        var de = List.of(
                cau(0, Difficulty.MEDIUM, false, 1),
                cau(1, Difficulty.HARD, null, 0),
                cau(2, Difficulty.EASY, null, 0));

        // Đổi hướng ngay câu đầu làm độ khó nhảy lên xuống từng câu; người học thấy đề "loạn" chứ không
        // thấy nó thích ứng. Nên vẫn theo thứ tự đề: câu số 1.
        assertThat(chon(de)).isEqualTo(de.get(1).questionId());
    }

    @Test
    @DisplayName("Sai HAI câu liền: chuyển sang câu DỄ nhất còn lại")
    void shouldGoEasierAfterTwoWrong() {
        var de = List.of(
                cau(0, Difficulty.MEDIUM, false, 1),
                cau(1, Difficulty.MEDIUM, false, 2),
                cau(2, Difficulty.HARD, null, 0),
                cau(3, Difficulty.EASY, null, 0));

        assertThat(chon(de)).as("người sai hai câu liền cần một câu dễ để lấy lại đà")
                .isEqualTo(de.get(3).questionId());
    }

    @Test
    @DisplayName("Đúng HAI câu liền: chuyển sang câu KHÓ nhất còn lại")
    void shouldGoHarderAfterTwoCorrect() {
        var de = List.of(
                cau(0, Difficulty.EASY, true, 1),
                cau(1, Difficulty.EASY, true, 2),
                cau(2, Difficulty.EASY, null, 0),
                cau(3, Difficulty.HARD, null, 0));

        assertThat(chon(de)).as("đã nắm rồi thì đừng phí thời gian với câu dễ nữa")
                .isEqualTo(de.get(3).questionId());
    }

    @Test
    @DisplayName("Đúng rồi sai: KHÔNG thành xu hướng, giữ nguyên thứ tự")
    void shouldKeepOrderWhenMixed() {
        var de = List.of(
                cau(0, Difficulty.EASY, true, 1),
                cau(1, Difficulty.MEDIUM, false, 2),
                cau(2, Difficulty.MEDIUM, null, 0),
                cau(3, Difficulty.HARD, null, 0));

        assertThat(chon(de)).isEqualTo(de.get(2).questionId());
    }

    @Test
    @DisplayName("Bẫy thật: thứ tự đề nói ĐÚNG-ĐÚNG, thứ tự làm nói SAI-SAI")
    void answerTimeOrderMustWin() {
        // Thứ tự ĐỀ, lọc câu đã làm: [0 sai, 1 sai, 5 đúng, 6 đúng] → hai cuối = ĐÚNG, ĐÚNG → khó hơn.
        // Thứ tự LÀM (theo thời gian):  [5 đúng, 6 đúng, 0 sai, 1 sai] → hai cuối = SAI, SAI → dễ hơn.
        // Hai cách đọc cho hai hướng NGƯỢC NHAU, nên test này chết ngay nếu ai đó bỏ phần sắp theo thời gian.
        var de = List.of(
                cau(0, Difficulty.MEDIUM, false, 30),   // làm gần nhất
                cau(1, Difficulty.MEDIUM, false, 40),   // làm gần nhất
                cau(2, Difficulty.HARD, null, 0),
                cau(3, Difficulty.EASY, null, 0),
                cau(5, Difficulty.EASY, true, 10),      // làm từ lâu
                cau(6, Difficulty.EASY, true, 20));     // làm từ lâu

        assertThat(AdaptiveNextPicker.huongTheoChuoi(de))
                .as("hai câu VỪA LÀM đều sai → phải dễ hơn, dù thứ tự đề nói ngược lại")
                .isEqualTo(AdaptiveNextPicker.Huong.DE_HON);

        assertThat(chon(de)).isEqualTo(de.get(3).questionId());   // câu EASY còn lại
    }

    @Test
    @DisplayName("Làm hết thì không còn câu nào để hỏi")
    void shouldReturnEmptyWhenDone() {
        var de = List.of(cau(0, Difficulty.EASY, true, 1), cau(1, Difficulty.EASY, true, 2));

        assertThat(AdaptiveNextPicker.chonTiepTheo(de)).isEmpty();
        assertThat(AdaptiveNextPicker.chonTiepTheo(List.of())).isEmpty();
        assertThat(AdaptiveNextPicker.chonTiepTheo(null)).isEmpty();
    }

    @Test
    @DisplayName("Câu chưa đặt độ khó nằm giữa thang, không rơi xuống đáy hay lên đỉnh")
    void shouldTreatNullDifficultyAsMedium() {
        // "Không biết độ khó" khác "dễ" và cũng khác "khó". Coi null là EASY thì mọi câu chưa phân loại
        // đều bị dồn cho người đang sai liên tiếp.
        var de = List.of(
                cau(0, Difficulty.MEDIUM, false, 1),
                cau(1, Difficulty.MEDIUM, false, 2),
                cau(2, null, null, 0),
                cau(3, Difficulty.EASY, null, 0));

        assertThat(chon(de)).isEqualTo(de.get(3).questionId());
    }

    @Test
    @DisplayName("BẤT BIẾN: chỉ chọn trong những câu CHƯA LÀM của chính đề này")
    void mustOnlyPickFromUnansweredInTheSamePaper() {
        // Bất biến trung tâm của FR-32. Nếu lớp này một ngày bắt đầu bỏ bớt hay thêm câu thì hai người học
        // có điểm không so được với nhau, và bảng xếp hạng / bảng theo dõi lớp / thống kê quiz đều sai theo.
        var de = List.of(
                cau(0, Difficulty.EASY, true, 1),
                cau(1, Difficulty.HARD, true, 2),
                cau(2, Difficulty.MEDIUM, null, 0),
                cau(3, Difficulty.HARD, null, 0));

        UUID chon = chon(de);

        assertThat(de.stream().map(AdaptiveNextPicker.Cau::questionId)).contains(chon);
        assertThat(de.stream().filter(AdaptiveNextPicker.Cau::chuaLam)
                .map(AdaptiveNextPicker.Cau::questionId)).contains(chon);
    }

    // ------------------------------------------------------------------ trợ giúp

    /** @param giay 0 = chưa làm; khác 0 = số giây kể từ mốc, dùng để dựng thứ tự LÀM */
    private static AdaptiveNextPicker.Cau cau(int thuTu, Difficulty doKho, Boolean ketQua, int giay) {
        return new AdaptiveNextPicker.Cau(UUID.randomUUID(), thuTu, doKho, ketQua,
                giay == 0 ? null : MOC.plusSeconds(giay));
    }

    private static UUID chon(List<AdaptiveNextPicker.Cau> de) {
        return AdaptiveNextPicker.chonTiepTheo(de).orElseThrow().questionId();
    }
}
