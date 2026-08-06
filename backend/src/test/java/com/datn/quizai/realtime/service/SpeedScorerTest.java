package com.datn.quizai.realtime.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test công thức tính điểm theo tốc độ (FR-22).
 * <p>
 * Tính chất quan trọng nhất cần bảo vệ: <b>trả lời đúng chậm vẫn hơn trả lời sai nhanh</b>.
 * Nếu công thức bị chỉnh sai, người chơi sẽ bấm bừa cho nhanh thay vì suy nghĩ.
 */
class SpeedScorerTest {

    private static final long LIMIT = 20_000L;

    @Test
    @DisplayName("Trả lời sai luôn 0 điểm, dù nhanh đến đâu")
    void shouldGiveNothingForWrongAnswer() {
        assertThat(SpeedScorer.score(1, false, 0, LIMIT)).isZero();
        assertThat(SpeedScorer.score(5, false, 100, LIMIT)).isZero();
    }

    @Test
    @DisplayName("Đúng tức thì được trọn điểm nền + trọn thưởng tốc độ")
    void shouldGiveMaxForInstantCorrect() {
        assertThat(SpeedScorer.score(1, true, 0, LIMIT))
                .isEqualTo(SpeedScorer.BASE + SpeedScorer.SPEED_BONUS);
    }

    @Test
    @DisplayName("Đúng sát giờ chót vẫn giữ được phần điểm nền")
    void shouldKeepBaseAtDeadline() {
        assertThat(SpeedScorer.score(1, true, LIMIT, LIMIT)).isEqualTo(SpeedScorer.BASE);
    }

    @Test
    @DisplayName("Đúng ở nửa thời gian được nền + nửa thưởng tốc độ")
    void shouldScaleLinearly() {
        assertThat(SpeedScorer.score(1, true, LIMIT / 2, LIMIT))
                .isEqualTo(SpeedScorer.BASE + SpeedScorer.SPEED_BONUS / 2);
    }

    @Test
    @DisplayName("Đúng chậm nhất vẫn hơn sai nhanh nhất — luật cốt lõi của cách tính điểm")
    void correctSlowShouldBeatWrongFast() {
        int correctAtDeadline = SpeedScorer.score(1, true, LIMIT, LIMIT);
        int wrongInstantly = SpeedScorer.score(1, false, 0, LIMIT);

        assertThat(correctAtDeadline).isGreaterThan(wrongInstantly);
    }

    @Test
    @DisplayName("Điểm nhân theo điểm gốc của câu hỏi")
    void shouldMultiplyByQuestionPoints() {
        assertThat(SpeedScorer.score(3, true, 0, LIMIT))
                .isEqualTo(3 * (SpeedScorer.BASE + SpeedScorer.SPEED_BONUS));
    }

    @Test
    @DisplayName("Trả lời sau khi hết giờ không được điểm")
    void shouldRejectLateAnswer() {
        assertThat(SpeedScorer.score(1, true, LIMIT + 1, LIMIT)).isZero();
    }

    @Test
    @DisplayName("Thời gian âm (lệch đồng hồ) không làm điểm vượt trần")
    void shouldClampNegativeElapsed() {
        assertThat(SpeedScorer.score(1, true, -5_000, LIMIT))
                .isEqualTo(SpeedScorer.BASE + SpeedScorer.SPEED_BONUS);
    }
}
