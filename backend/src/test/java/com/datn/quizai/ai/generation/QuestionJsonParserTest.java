package com.datn.quizai.ai.generation;

import com.datn.quizai.quiz.domain.Difficulty;
import com.datn.quizai.quiz.domain.QuestionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test lớp kiểm duyệt JSON do mô hình trả về.
 * <p>
 * Đây là chốt chặn giữa "AI nói gì đó" và "câu hỏi vào ngân hàng đề". Mô hình ngôn ngữ không cam
 * kết đúng định dạng, nên mọi ca lệch ở đây đều là ca <b>sẽ xảy ra</b> chứ không phải giả định.
 */
class QuestionJsonParserTest {

    private static final String VALID_SINGLE = """
            {"questions":[{
              "type":"SINGLE_CHOICE",
              "question":"Thủ đô Việt Nam là thành phố nào?",
              "options":["Hà Nội","Huế","Đà Nẵng","Cần Thơ"],
              "correctAnswer":"Hà Nội",
              "explanation":"Hà Nội là thủ đô từ năm 1945.",
              "difficulty":"EASY",
              "topic":"Địa lý"
            }]}
            """;

    @Nested
    @DisplayName("Đọc đúng định dạng chuẩn")
    class HappyPath {

        @Test
        @DisplayName("Đọc được câu một đáp án đầy đủ trường")
        void shouldParseSingleChoice() {
            var result = QuestionJsonParser.parse(VALID_SINGLE);

            assertThat(result.rejected()).isEmpty();
            assertThat(result.questions()).hasSize(1);

            GeneratedQuestion question = result.questions().get(0);
            assertThat(question.type()).isEqualTo(QuestionType.SINGLE_CHOICE);
            assertThat(question.difficulty()).isEqualTo(Difficulty.EASY);
            assertThat(question.topic()).isEqualTo("Địa lý");
            assertThat(question.options()).hasSize(4);
            assertThat(question.options()).filteredOn(GeneratedQuestion.Option::correct)
                    .extracting(GeneratedQuestion.Option::content).containsExactly("Hà Nội");
        }

        @Test
        @DisplayName("Chấp nhận cả dạng options là mảng object {content, correct}")
        void shouldParseObjectOptions() {
            var result = QuestionJsonParser.parse("""
                    {"questions":[{
                      "type":"TRUE_FALSE","question":"HTTP là giao thức phi trạng thái?",
                      "options":[{"content":"Đúng","correct":true},{"content":"Sai","correct":false}],
                      "explanation":"HTTP không giữ trạng thái giữa các request."
                    }]}
                    """);

            assertThat(result.questions()).hasSize(1);
            assertThat(result.questions().get(0).options()).hasSize(2);
        }

        @Test
        @DisplayName("Câu nhiều đáp án nhận correctAnswer dạng mảng")
        void shouldParseMultipleCorrectAnswers() {
            var result = QuestionJsonParser.parse("""
                    {"questions":[{
                      "type":"MULTIPLE_CHOICE","question":"Ngôn ngữ nào chạy trên JVM?",
                      "options":["Java","Kotlin","Python","Ruby"],
                      "correctAnswer":["Java","Kotlin"],
                      "explanation":"Java và Kotlin biên dịch ra bytecode JVM."
                    }]}
                    """);

            assertThat(result.questions()).hasSize(1);
            assertThat(result.questions().get(0).options())
                    .filteredOn(GeneratedQuestion.Option::correct).hasSize(2);
        }

        @Test
        @DisplayName("Câu điền khuyết lấy correctAnswer làm các đáp án được chấp nhận")
        void shouldParseFillBlank() {
            var result = QuestionJsonParser.parse("""
                    {"questions":[{
                      "type":"FILL_BLANK","question":"Giao thức truyền siêu văn bản viết tắt là ___",
                      "options":[],"correctAnswer":["HTTP","http"],
                      "explanation":"HyperText Transfer Protocol."
                    }]}
                    """);

            assertThat(result.questions().get(0).options()).hasSize(2);
            assertThat(result.questions().get(0).options())
                    .allSatisfy(option -> assertThat(option.correct()).isTrue());
        }
    }

    @Nested
    @DisplayName("Chịu được đầu ra lệch chuẩn của mô hình")
    class Tolerance {

        @Test
        @DisplayName("Gỡ được khối ```json mà mô hình hay bọc quanh dù đã yêu cầu JSON thuần")
        void shouldStripCodeFence() {
            String fenced = "```json\n" + VALID_SINGLE + "\n```";

            assertThat(QuestionJsonParser.parse(fenced).questions()).hasSize(1);
        }

        @Test
        @DisplayName("Chấp nhận mô hình trả thẳng mảng thay vì bọc trong {\"questions\": [...]}")
        void shouldAcceptBareArray() {
            var result = QuestionJsonParser.parse("""
                    [{"type":"SINGLE_CHOICE","question":"1 + 1 = ?",
                      "options":["2","3"],"correctAnswer":"2","explanation":"Hiển nhiên."}]
                    """);

            assertThat(result.questions()).hasSize(1);
        }

        @Test
        @DisplayName("Tên loại viết thường hoặc có gạch nối vẫn nhận ra")
        void shouldNormalizeTypeName() {
            var result = QuestionJsonParser.parse("""
                    {"questions":[{"type":"single-choice","question":"1 + 1 = ?",
                      "options":["2","3"],"correctAnswer":"2","explanation":"x"}]}
                    """);

            assertThat(result.questions().get(0).type()).isEqualTo(QuestionType.SINGLE_CHOICE);
        }

        @Test
        @DisplayName("Thiếu hoặc sai difficulty thì mặc định MEDIUM, không loại cả câu")
        void shouldDefaultDifficulty() {
            var result = QuestionJsonParser.parse("""
                    {"questions":[{"type":"SINGLE_CHOICE","question":"1 + 1 = ?",
                      "options":["2","3"],"correctAnswer":"2","explanation":"x","difficulty":"siêu khó"}]}
                    """);

            assertThat(result.questions().get(0).difficulty()).isEqualTo(Difficulty.MEDIUM);
        }

        @Test
        @DisplayName("JSON hỏng hẳn thì ném lỗi rõ ràng")
        void shouldFailOnBrokenJson() {
            assertThatThrownBy(() -> QuestionJsonParser.parse("không phải JSON"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("JSON");
        }
    }

    @Nested
    @DisplayName("Loại câu hỏi vi phạm luật, giữ lại câu tốt")
    class Validation {

        @Test
        @DisplayName("Câu một đáp án mà không có đáp án đúng nào bị loại")
        void shouldRejectSingleChoiceWithoutCorrectAnswer() {
            var result = QuestionJsonParser.parse("""
                    {"questions":[{"type":"SINGLE_CHOICE","question":"Câu hỏi sai luật",
                      "options":["A","B","C"],"correctAnswer":"D","explanation":"x"}]}
                    """);

            assertThat(result.questions()).isEmpty();
            assertThat(result.rejected()).hasSize(1);
            assertThat(result.rejected().get(0)).contains("1 đáp án đúng");
        }

        @Test
        @DisplayName("Câu nhiều đáp án mà tất cả lựa chọn đều đúng bị loại")
        void shouldRejectMultipleChoiceWithoutWrongOption() {
            var result = QuestionJsonParser.parse("""
                    {"questions":[{"type":"MULTIPLE_CHOICE","question":"Câu sai luật",
                      "options":["A","B","C"],"correctAnswer":["A","B","C"],"explanation":"x"}]}
                    """);

            assertThat(result.questions()).isEmpty();
            assertThat(result.rejected().get(0)).contains("≥1 lựa chọn sai");
        }

        @Test
        @DisplayName("Câu Đúng/Sai có 3 lựa chọn bị loại")
        void shouldRejectTrueFalseWithThreeOptions() {
            var result = QuestionJsonParser.parse("""
                    {"questions":[{"type":"TRUE_FALSE","question":"Câu sai luật",
                      "options":["Đúng","Sai","Không biết"],"correctAnswer":"Đúng","explanation":"x"}]}
                    """);

            assertThat(result.questions()).isEmpty();
        }

        @Test
        @DisplayName("Loại câu hỏi lạ bị loại, không làm hỏng cả mẻ")
        void shouldRejectUnknownType() {
            var result = QuestionJsonParser.parse("""
                    {"questions":[{"type":"ESSAY_LONG","question":"Câu loại lạ",
                      "options":[],"correctAnswer":"x","explanation":"x"}]}
                    """);

            assertThat(result.questions()).isEmpty();
            assertThat(result.rejected().get(0)).contains("không hợp lệ");
        }

        @Test
        @DisplayName("Một câu hỏng KHÔNG làm mất những câu tốt còn lại")
        void shouldKeepGoodQuestionsWhenOneIsBroken() {
            var result = QuestionJsonParser.parse("""
                    {"questions":[
                      {"type":"SINGLE_CHOICE","question":"Câu tốt số 1",
                       "options":["A","B"],"correctAnswer":"A","explanation":"x"},
                      {"type":"SINGLE_CHOICE","question":"Câu hỏng",
                       "options":["A","B"],"correctAnswer":"Z","explanation":"x"},
                      {"type":"SINGLE_CHOICE","question":"Câu tốt số 2",
                       "options":["A","B"],"correctAnswer":"B","explanation":"x"}
                    ]}
                    """);

            assertThat(result.questions()).hasSize(2);
            assertThat(result.rejected()).hasSize(1);
            assertThat(result.questions()).extracting(GeneratedQuestion::content)
                    .containsExactly("Câu tốt số 1", "Câu tốt số 2");
        }

        @Test
        @DisplayName("Câu trùng nội dung bị lọc — mô hình rất hay diễn đạt lại cùng một ý")
        void shouldDeduplicate() {
            var result = QuestionJsonParser.parse("""
                    {"questions":[
                      {"type":"SINGLE_CHOICE","question":"Thủ đô Việt Nam là gì?",
                       "options":["Hà Nội","Huế"],"correctAnswer":"Hà Nội","explanation":"x"},
                      {"type":"SINGLE_CHOICE","question":"thủ đô   Việt Nam LÀ GÌ?",
                       "options":["Hà Nội","Huế"],"correctAnswer":"Hà Nội","explanation":"x"}
                    ]}
                    """);

            assertThat(result.questions()).hasSize(1);
            assertThat(result.rejected().get(0)).contains("Trùng nội dung");
        }

        @Test
        @DisplayName("Câu thiếu nội dung bị loại")
        void shouldRejectEmptyContent() {
            var result = QuestionJsonParser.parse("""
                    {"questions":[{"type":"SINGLE_CHOICE","question":"  ",
                      "options":["A","B"],"correctAnswer":"A","explanation":"x"}]}
                    """);

            assertThat(result.questions()).isEmpty();
            assertThat(result.rejected().get(0)).contains("Thiếu nội dung");
        }
    }
}
