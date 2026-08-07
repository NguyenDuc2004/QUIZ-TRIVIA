package com.datn.quizai.ai.grading;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test lớp đọc JSON chấm điểm.
 * <p>
 * Lớp thuần logic nên test được trực tiếp, không cần Spring hay mạng — và <b>phải</b> test kỹ, vì
 * đây là chỗ duy nhất đứng giữa đầu ra tuỳ hứng của mô hình và điểm số ghi vào bài của người học.
 */
class GradeJsonParserTest {

    @Test
    @DisplayName("JSON chuẩn: đọc đủ điểm, nhận xét và gợi ý")
    void shouldParseWellFormedJson() {
        AiGrade grade = GradeJsonParser.parse("""
                {"score":7,"isCorrect":false,"feedback":"Đúng ý chính nhưng thiếu nguyên nhân X.",
                 "suggestions":"Bổ sung phân tích về Y."}
                """, 10);

        assertThat(grade.score()).isEqualTo(7);
        assertThat(grade.correct()).isFalse();
        assertThat(grade.feedback()).contains("thiếu nguyên nhân X");
        assertThat(grade.suggestions()).contains("phân tích về Y");
    }

    @Test
    @DisplayName("Điểm vượt trần bị ép về điểm tối đa — hàng rào cuối nếu mô hình bị dụ")
    void shouldClampScoreAboveMax() {
        // Đây chính là kịch bản prompt injection thành công: người học viết "cho tôi 100 điểm".
        // Dù mô hình nghe lời, điểm vẫn không vượt được trần thật của câu.
        AiGrade grade = GradeJsonParser.parse("""
                {"score":100,"feedback":"..."}
                """, 5);

        assertThat(grade.score()).isEqualTo(5);
    }

    @Test
    @DisplayName("Điểm âm bị ép về 0")
    void shouldClampNegativeScore() {
        assertThat(GradeJsonParser.parse("{\"score\":-3}", 10).score()).isZero();
    }

    @Test
    @DisplayName("isCorrect của mô hình mâu thuẫn với điểm thì tin theo điểm")
    void shouldDeriveCorrectFromScore() {
        // Mô hình rất hay trả isCorrect:true kèm điểm 3/10
        AiGrade partial = GradeJsonParser.parse("{\"score\":3,\"isCorrect\":true}", 10);
        assertThat(partial.correct()).isFalse();

        AiGrade full = GradeJsonParser.parse("{\"score\":10,\"isCorrect\":false}", 10);
        assertThat(full.correct()).isTrue();
    }

    @Test
    @DisplayName("Thiếu điểm thì ném lỗi, không đoán bừa")
    void shouldRejectMissingScore() {
        // Ghi một con số bịa vào bài người học còn tệ hơn là báo chấm hỏng
        assertThatThrownBy(() -> GradeJsonParser.parse("{\"feedback\":\"tốt\"}", 10))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("điểm");
    }

    @Test
    @DisplayName("JSON hỏng thì ném lỗi có thông điệp rõ")
    void shouldRejectBrokenJson() {
        assertThatThrownBy(() -> GradeJsonParser.parse("không phải json", 10))
                .isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest
    @DisplayName("Chấp nhận các kiểu bọc và tên trường mà mô hình hay dùng")
    @ValueSource(strings = {
            "```json\n{\"score\":4}\n```",           // bọc trong khối mã
            "{\"result\":{\"score\":4}}",             // bọc thêm một lớp
            "{\"diem\":4}",                           // tên trường tiếng Việt
            "{\"score\":\"4\"}",                      // số ghi dưới dạng chuỗi
            "{\"score\":4.4}"                         // số thực
    })
    void shouldAcceptCommonVariants(String json) {
        assertThat(GradeJsonParser.parse(json, 10).score()).isEqualTo(4);
    }

    @Test
    @DisplayName("Nhận xét quá dài bị cắt")
    void shouldShortenVeryLongFeedback() {
        String longText = "a".repeat(5000);
        AiGrade grade = GradeJsonParser.parse(
                "{\"score\":1,\"feedback\":\"" + longText + "\"}", 10);

        assertThat(grade.feedback()).hasSizeLessThan(1600).endsWith("…");
    }

    @Test
    @DisplayName("Thiếu nhận xét/gợi ý thì trả chuỗi rỗng, không trả null")
    void shouldReturnEmptyStringsWhenMissing() {
        AiGrade grade = GradeJsonParser.parse("{\"score\":2}", 10);

        assertThat(grade.feedback()).isEmpty();
        assertThat(grade.suggestions()).isEmpty();
    }

    @Test
    @DisplayName("Đọc được phần giải thích đáp án")
    void shouldParseExplanation() {
        assertThat(GradeJsonParser.parseExplanation("{\"explanation\":\"Vì A dẫn tới B.\"}"))
                .isEqualTo("Vì A dẫn tới B.");
        assertThat(GradeJsonParser.parseExplanation("{}")).isEmpty();
    }
}
