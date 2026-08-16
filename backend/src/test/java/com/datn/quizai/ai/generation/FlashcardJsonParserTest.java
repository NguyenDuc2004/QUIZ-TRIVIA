package com.datn.quizai.ai.generation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Lọc kết quả mô hình khi sinh thẻ (features/11, FR-38).
 * <p>
 * Đây là lớp duy nhất đứng giữa đầu ra của mô hình và bộ thẻ của người học, nên test nhắm vào những gì mô
 * hình <b>thật sự</b> trả về sai — không phải nhắm vào việc đọc JSON đúng cú pháp.
 */
class FlashcardJsonParserTest {

    @Test
    @DisplayName("Đọc được danh sách thẻ bình thường, kèm gợi ý")
    void parsesValidCards() {
        var kq = FlashcardJsonParser.parse("""
                {"flashcards":[
                  {"front":"IoC container là gì?","back":"Nơi Spring quản lý vòng đời và phụ thuộc của bean","hint":"Spring core"},
                  {"front":"@Transactional làm gì?","back":"Bọc phương thức trong một giao dịch"}
                ]}
                """);

        assertThat(kq.flashcards()).hasSize(2);
        assertThat(kq.rejected()).isEmpty();
        assertThat(kq.flashcards().get(0).hint()).isEqualTo("Spring core");
        // Không có gợi ý thì để null, KHÔNG để chuỗi rỗng — giao diện phân biệt hai thứ đó
        assertThat(kq.flashcards().get(1).hint()).isNull();
    }

    @Test
    @DisplayName("Chấp nhận cả khi mô hình trả mảng trần, không bọc trong {flashcards:[...]}")
    void acceptsBareArray() {
        var kq = FlashcardJsonParser.parse("""
                [{"front":"HTTP 404 nghĩa là gì?","back":"Không tìm thấy tài nguyên"}]
                """);
        assertThat(kq.flashcards()).hasSize(1);
    }

    @Test
    @DisplayName("Bỏ khối ``` mà mô hình hay bọc quanh JSON")
    void stripsCodeFence() {
        var kq = FlashcardJsonParser.parse("""
                ```json
                {"flashcards":[{"front":"REST là gì?","back":"Một kiểu kiến trúc cho API trên HTTP"}]}
                ```
                """);
        assertThat(kq.flashcards()).hasSize(1);
    }

    @Test
    @DisplayName("Loại thẻ thiếu một mặt, và nói rõ lý do")
    void rejectsOneSidedCards() {
        var kq = FlashcardJsonParser.parse("""
                {"flashcards":[
                  {"front":"Câu hỏi không có đáp án","back":""},
                  {"front":"","back":"Đáp án không có câu hỏi"},
                  {"front":"Thẻ đủ hai mặt","back":"Nội dung hợp lệ"}
                ]}
                """);

        assertThat(kq.flashcards()).hasSize(1);
        assertThat(kq.rejected()).hasSize(2);
        // Lý do phải nói được thẻ nào bị loại, không chỉ "có 2 thẻ bị loại"
        assertThat(kq.rejected().get(0)).contains("Thiếu mặt trước hoặc mặt sau");
    }

    @Test
    @DisplayName("Loại thẻ có mặt sau dài như một đoạn giảng bài")
    void rejectsEssayLikeBack() {
        String doanVan = "Nội dung rất dài. ".repeat(40);   // > 400 ký tự
        var kq = FlashcardJsonParser.parse("""
                {"flashcards":[
                  {"front":"Giải thích toàn bộ Spring Framework","back":"%s"},
                  {"front":"Bean scope mặc định?","back":"singleton"}
                ]}
                """.formatted(doanVan));

        // Đây là cách hỏng đặc trưng nhất: mô hình chuyển sang giảng bài, và thẻ mất đúng cái làm nên
        // flashcard — đọc trong vài giây rồi tự đối chiếu.
        assertThat(kq.flashcards()).hasSize(1);
        assertThat(kq.flashcards().get(0).front()).isEqualTo("Bean scope mặc định?");
        assertThat(kq.rejected().get(0)).contains("Mặt sau quá dài");
    }

    @Test
    @DisplayName("Loại thẻ trùng mặt trước, kể cả khi chỉ khác hoa thường và khoảng trắng")
    void rejectsDuplicateFronts() {
        var kq = FlashcardJsonParser.parse("""
                {"flashcards":[
                  {"front":"Dependency Injection là gì?","back":"Đưa phụ thuộc từ ngoài vào"},
                  {"front":"dependency   injection LÀ GÌ?","back":"Cách khác diễn đạt cùng một điều"},
                  {"front":"AOP là gì?","back":"Lập trình hướng khía cạnh"}
                ]}
                """);

        // Không chuẩn hoá thì hai thẻ này lọt cả hai, và lịch SRS nhân đôi công ôn cho cùng một kiến thức
        assertThat(kq.flashcards()).hasSize(2);
        assertThat(kq.rejected()).hasSize(1);
        assertThat(kq.rejected().get(0)).contains("Trùng mặt trước");
    }

    @Test
    @DisplayName("Đọc được cả khi mô hình đặt tên trường khác (question/answer)")
    void acceptsAlternativeFieldNames() {
        // Mỗi mô hình đặt tên một kiểu, và đây là lý do AiJson.text nhận nhiều tên
        var kq = FlashcardJsonParser.parse("""
                {"flashcards":[{"question":"Kế thừa là gì?","answer":"Lớp con nhận thuộc tính của lớp cha"}]}
                """);
        assertThat(kq.flashcards()).hasSize(1);
        assertThat(kq.flashcards().get(0).front()).isEqualTo("Kế thừa là gì?");
    }

    @Test
    @DisplayName("JSON không phải danh sách thì ném lỗi, không trả rỗng lặng lẽ")
    void throwsOnNonList() {
        // Trả rỗng lặng lẽ thì tầng trên báo "AI không tạo được thẻ nào" — thông báo đó dẫn người dùng đi
        // sửa chủ đề, trong khi lỗi thật là mô hình trả về sai cấu trúc.
        assertThatThrownBy(() -> FlashcardJsonParser.parse("{\"ket_qua\":\"khong co gi\"}"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("không phải danh sách");
    }
}
