package com.datn.quizai.attempt.service;

import com.datn.quizai.attempt.domain.AnswerPayload;
import com.datn.quizai.attempt.domain.GradedBy;
import com.datn.quizai.quiz.domain.Question;
import com.datn.quizai.quiz.domain.QuestionOption;
import com.datn.quizai.quiz.domain.QuestionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test luật chấm điểm tự động (FR-15).
 * <p>
 * Lớp {@link AnswerGrader} thuần logic nên test trực tiếp, không cần Spring hay CSDL —
 * đây là chỗ dễ sai nhất của tính năng làm bài nên phủ kỹ từng loại câu hỏi.
 */
class AnswerGraderTest {

    /** Dựng câu hỏi kèm lựa chọn đã có id sẵn (bình thường JPA sinh khi lưu). */
    private static Question question(QuestionType type, int points, String... optionSpecs) {
        Question question = new Question();
        question.setType(type);
        question.setPoints(points);

        for (String spec : optionSpecs) {
            boolean correct = spec.startsWith("*");
            QuestionOption option = new QuestionOption(correct ? spec.substring(1) : spec, correct);
            option.setId(UUID.randomUUID());
            question.addOption(option);
        }
        return question;
    }

    private static UUID idOf(Question question, String content) {
        return question.getOptions().stream()
                .filter(option -> option.getContent().equals(content))
                .findFirst().orElseThrow().getId();
    }

    @Nested
    @DisplayName("Câu một đáp án và Đúng/Sai")
    class SingleChoice {

        @Test
        @DisplayName("Chọn đúng → được trọn điểm của câu")
        void shouldAwardFullPoints() {
            Question q = question(QuestionType.SINGLE_CHOICE, 3, "*Hà Nội", "Huế");

            var result = AnswerGrader.grade(q, AnswerPayload.ofOptions(List.of(idOf(q, "Hà Nội"))));

            assertThat(result.correct()).isTrue();
            assertThat(result.score()).isEqualTo(3);
            assertThat(result.gradedBy()).isEqualTo(GradedBy.AUTO);
        }

        @Test
        @DisplayName("Chọn sai → 0 điểm, đánh dấu sai")
        void shouldRejectWrongOption() {
            Question q = question(QuestionType.SINGLE_CHOICE, 3, "*Hà Nội", "Huế");

            var result = AnswerGrader.grade(q, AnswerPayload.ofOptions(List.of(idOf(q, "Huế"))));

            assertThat(result.correct()).isFalse();
            assertThat(result.score()).isZero();
        }

        @Test
        @DisplayName("Chọn cả đúng lẫn sai ở câu một đáp án → tính sai")
        void shouldRejectMultipleSelection() {
            Question q = question(QuestionType.SINGLE_CHOICE, 3, "*Hà Nội", "Huế");

            var result = AnswerGrader.grade(q,
                    AnswerPayload.ofOptions(List.of(idOf(q, "Hà Nội"), idOf(q, "Huế"))));

            assertThat(result.correct()).isFalse();
        }

        @Test
        @DisplayName("Câu Đúng/Sai chấm theo cùng luật")
        void shouldGradeTrueFalse() {
            Question q = question(QuestionType.TRUE_FALSE, 1, "*Đúng", "Sai");

            assertThat(AnswerGrader.grade(q, AnswerPayload.ofOptions(List.of(idOf(q, "Đúng")))).correct()).isTrue();
            assertThat(AnswerGrader.grade(q, AnswerPayload.ofOptions(List.of(idOf(q, "Sai")))).correct()).isFalse();
        }
    }

    @Nested
    @DisplayName("Câu nhiều đáp án — chấm trọn gói")
    class MultipleChoice {

        @Test
        @DisplayName("Chọn đúng và đủ → trọn điểm")
        void shouldAwardWhenExactMatch() {
            Question q = question(QuestionType.MULTIPLE_CHOICE, 4, "*Java", "*Kotlin", "Python");

            var result = AnswerGrader.grade(q,
                    AnswerPayload.ofOptions(List.of(idOf(q, "Kotlin"), idOf(q, "Java"))));

            assertThat(result.correct()).isTrue();
            assertThat(result.score()).isEqualTo(4);
        }

        @Test
        @DisplayName("Chọn thiếu một đáp án đúng → 0 điểm (không chấm từng phần)")
        void shouldRejectPartialSelection() {
            Question q = question(QuestionType.MULTIPLE_CHOICE, 4, "*Java", "*Kotlin", "Python");

            var result = AnswerGrader.grade(q, AnswerPayload.ofOptions(List.of(idOf(q, "Java"))));

            assertThat(result.correct()).isFalse();
            assertThat(result.score()).isZero();
        }

        @Test
        @DisplayName("Chọn đủ đáp án đúng nhưng thừa một đáp án sai → 0 điểm")
        void shouldRejectExtraSelection() {
            Question q = question(QuestionType.MULTIPLE_CHOICE, 4, "*Java", "*Kotlin", "Python");

            var result = AnswerGrader.grade(q, AnswerPayload.ofOptions(
                    List.of(idOf(q, "Java"), idOf(q, "Kotlin"), idOf(q, "Python"))));

            assertThat(result.correct()).isFalse();
        }
    }

    @Nested
    @DisplayName("Câu điền khuyết")
    class FillBlank {

        @Test
        @DisplayName("Bỏ qua hoa/thường và khoảng trắng thừa")
        void shouldNormalizeCaseAndWhitespace() {
            Question q = question(QuestionType.FILL_BLANK, 2, "*SQL");

            assertThat(AnswerGrader.grade(q, AnswerPayload.ofText("  sQl ")).correct()).isTrue();
            assertThat(AnswerGrader.grade(q, AnswerPayload.ofText("SQL")).correct()).isTrue();
        }

        @Test
        @DisplayName("Gộp nhiều khoảng trắng giữa các từ thành một")
        void shouldCollapseInnerWhitespace() {
            Question q = question(QuestionType.FILL_BLANK, 2, "*ngôn ngữ SQL");

            assertThat(AnswerGrader.grade(q, AnswerPayload.ofText("ngôn    ngữ   SQL")).correct()).isTrue();
        }

        @Test
        @DisplayName("Khớp bất kỳ đáp án nào trong danh sách được chấp nhận")
        void shouldAcceptAnyListedAnswer() {
            Question q = question(QuestionType.FILL_BLANK, 2, "*SQL", "*ngôn ngữ SQL");

            assertThat(AnswerGrader.grade(q, AnswerPayload.ofText("ngôn ngữ SQL")).correct()).isTrue();
        }

        @Test
        @DisplayName("KHÔNG bỏ dấu tiếng Việt: 'toan' không được tính là 'toán'")
        void shouldKeepVietnameseDiacritics() {
            Question q = question(QuestionType.FILL_BLANK, 2, "*toán");

            assertThat(AnswerGrader.grade(q, AnswerPayload.ofText("toan")).correct()).isFalse();
        }
    }

    @Nested
    @DisplayName("Câu tự luận và câu bỏ trống")
    class ShortAnswerAndBlank {

        @Test
        @DisplayName("Tự luận có trả lời → chuyển sang chờ AI chấm, chưa kết luận đúng/sai")
        void shouldDeferToAi() {
            Question q = question(QuestionType.SHORT_ANSWER, 5, "*REST dựa trên tài nguyên và HTTP");

            var result = AnswerGrader.grade(q, AnswerPayload.ofText("REST dùng HTTP"));

            assertThat(result.gradedBy()).isEqualTo(GradedBy.PENDING_AI);
            assertThat(result.correct()).isNull();
            assertThat(result.score()).isZero();
        }

        @Test
        @DisplayName("Tự luận bỏ trống → chốt sai ngay, không tốn lượt gọi AI")
        void shouldNotSendBlankShortAnswerToAi() {
            Question q = question(QuestionType.SHORT_ANSWER, 5, "*REST dựa trên tài nguyên và HTTP");

            var result = AnswerGrader.grade(q, AnswerPayload.ofText("   "));

            assertThat(result.gradedBy()).isEqualTo(GradedBy.AUTO);
            assertThat(result.correct()).isFalse();
        }

        @Test
        @DisplayName("Không trả lời (payload null hoặc rỗng) → 0 điểm, tính sai")
        void shouldGradeMissingAnswerAsWrong() {
            Question q = question(QuestionType.SINGLE_CHOICE, 3, "*Hà Nội", "Huế");

            assertThat(AnswerGrader.grade(q, null).correct()).isFalse();
            assertThat(AnswerGrader.grade(q, AnswerPayload.ofOptions(List.of())).score()).isZero();
        }
    }

    @Test
    @DisplayName("Câu không đặt điểm → mặc định 1 điểm")
    void shouldDefaultToOnePoint() {
        Question q = question(QuestionType.SINGLE_CHOICE, 1, "*A", "B");
        q.setPoints(null);

        assertThat(AnswerGrader.grade(q, AnswerPayload.ofOptions(List.of(idOf(q, "A")))).score()).isEqualTo(1);
    }
}
