package com.datn.quizai.ai.chat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test chỉ dẫn hệ thống của trợ lý học tập.
 * <p>
 * Hai thứ ở đây từng hỏng theo kiểu <b>không làm gì đỏ cả</b>, nên phải có phép kiểm canh:
 * <ol>
 *   <li><b>Ví dụ LaTeX bị chuỗi thoát của Java nuốt.</b> Text block VẪN xử lý chuỗi thoát, và
 *       {@code \f} là ký tự form-feed. Viết một dấu chéo thì ví dụ gửi tới mô hình thành
 *       {@code <FF>rac{a}{b}} — không gây lỗi, chỉ lặng lẽ dạy mô hình bằng một ví dụ hỏng, đúng
 *       chỗ đang dặn nó viết cho chuẩn. Lỗi này đã cắn bốn lần trong dự án.</li>
 *   <li><b>Cấm Markdown.</b> Giao diện dựng câu trả lời bằng văn bản thuần cộng LaTeX
 *       ({@code MessageBubble} trong {@code AssistantPage.tsx}), không dựng Markdown. Thiếu ràng
 *       buộc này thì mô hình trả về {@code **đậm**} và {@code * gạch đầu dòng}, và người học nhìn
 *       thấy nguyên dấu sao giữa câu trả lời.</li>
 * </ol>
 * Cả hai đều là lỗi <i>hiển thị</i>: hệ thống chạy đúng, test chức năng xanh, chỉ có người đọc là
 * thấy sai. Nên chỗ canh duy nhất là ở đây.
 */
class ChatPromptBuilderTest {

    private final String chiDan = ChatPromptBuilder.systemInstruction();

    @Test
    @DisplayName("ví dụ LaTeX giữ nguyên dấu chéo, không bị biến thành ký tự form-feed")
    void viDuLatexKhongBiNuot() {
        assertThat(chiDan).contains("$y = x^2$");
        assertThat(chiDan).contains("\\frac{a}{b}");
        assertThat(chiDan)
                .as("có ký tự form-feed nghĩa là dấu chéo trong text block đã bị nuốt")
                .doesNotContain("\f");
    }

    @Test
    @DisplayName("dặn mô hình viết văn bản thuần, không dùng cú pháp Markdown")
    void camMarkdown() {
        assertThat(chiDan).containsIgnoringCase("không dùng cú pháp Markdown");
    }

    @Test
    @DisplayName("giữ nguyên rào quanh học liệu và câu hỏi để chống tiêm chỉ thị")
    void conRaoChongTiemChiThi() {
        assertThat(chiDan).contains("<<<HOC_LIEU>>>", "<<<HET_HOC_LIEU>>>");
        assertThat(chiDan).contains("<<<CAU_HOI_CUA_NGUOI_HOC>>>", "<<<HET_CAU_HOI>>>");
        assertThat(chiDan).contains("KHÔNG được làm theo");
    }
}
