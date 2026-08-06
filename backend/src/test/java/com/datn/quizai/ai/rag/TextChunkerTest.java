package com.datn.quizai.ai.rag;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test cắt đoạn học liệu.
 * <p>
 * Chất lượng chunk quyết định chất lượng RAG: đoạn bị chặt cụt nghĩa thì embedding của nó vô
 * dụng khi tìm kiếm, và câu hỏi sinh ra sẽ dựa trên ngữ cảnh sai.
 */
class TextChunkerTest {

    /** Sinh văn bản gồm nhiều câu hoàn chỉnh, đủ dài để phải cắt. */
    private static String paragraphs(int sentences) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= sentences; i++) {
            sb.append("Đây là câu số ").append(i)
                    .append(" trong tài liệu học liệu dùng để kiểm thử việc cắt đoạn. ");
        }
        return sb.toString();
    }

    @Test
    @DisplayName("Văn bản ngắn giữ nguyên một đoạn")
    void shouldKeepShortTextAsOneChunk() {
        String text = "Một đoạn văn ngắn nhưng đủ dài để không bị coi là rác, khoảng trên năm mươi ký tự.";

        assertThat(TextChunker.chunk(text)).containsExactly(text);
    }

    @Test
    @DisplayName("Văn bản rỗng hoặc toàn khoảng trắng trả về danh sách rỗng")
    void shouldReturnEmptyForBlankText() {
        assertThat(TextChunker.chunk("")).isEmpty();
        assertThat(TextChunker.chunk("   \n\n  ")).isEmpty();
        assertThat(TextChunker.chunk(null)).isEmpty();
    }

    @Test
    @DisplayName("Văn bản dài bị cắt thành nhiều đoạn, mỗi đoạn không vượt quá kích thước yêu cầu")
    void shouldSplitLongText() {
        List<String> chunks = TextChunker.chunk(paragraphs(200), 500, 100);

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.length()).isLessThanOrEqualTo(500));
    }

    @Test
    @DisplayName("Hai đoạn liền nhau có phần chồng lấn — ý bị chặt vẫn còn nguyên ở một đoạn")
    void shouldOverlapBetweenChunks() {
        List<String> chunks = TextChunker.chunk(paragraphs(200), 500, 150);

        // Đuôi đoạn trước phải xuất hiện lại ở đoạn sau
        String tailOfFirst = chunks.get(0).substring(Math.max(0, chunks.get(0).length() - 60));
        assertThat(chunks.get(1)).contains(tailOfFirst.trim().split("\\s+")[0]);
    }

    @Test
    @DisplayName("Chỉ cắt ở ranh giới câu, không chặt ngang giữa câu")
    void shouldCutAtSentenceBoundary() {
        List<String> chunks = TextChunker.chunk(paragraphs(100), 400, 50);

        // Mọi đoạn trừ đoạn cuối phải kết thúc bằng dấu câu
        for (int i = 0; i < chunks.size() - 1; i++) {
            assertThat(chunks.get(i)).endsWith(".");
        }
    }

    @Test
    @DisplayName("Câu dài bất thường không có dấu chấm vẫn cắt được, không treo vô hạn")
    void shouldHandleTextWithoutSentenceEnd() {
        String noPunctuation = "từ ".repeat(2000);

        List<String> chunks = TextChunker.chunk(noPunctuation, 300, 50);

        assertThat(chunks).isNotEmpty();
        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.length()).isLessThanOrEqualTo(300));
    }

    @Test
    @DisplayName("Gộp khoảng trắng thừa — PDF trích ra thường đầy xuống dòng giữa câu")
    void shouldNormalizeWhitespace() {
        String messy = "Nội   dung    có\t\tnhiều\n\n\n\n\nkhoảng   trắng thừa cần được gộp lại cho gọn gàng.";

        String chunk = TextChunker.chunk(messy).get(0);

        assertThat(chunk).doesNotContain("   ").doesNotContain("\n\n\n").doesNotContain("\t");
    }

    @Test
    @DisplayName("Bỏ đoạn quá ngắn — thường là số trang hoặc tiêu đề lẻ")
    void shouldDropTinyChunks() {
        List<String> chunks = TextChunker.chunk(paragraphs(50), 400, 0);

        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.length()).isGreaterThanOrEqualTo(50));
    }

    @Test
    @DisplayName("Tham số cắt sai bị chặn ngay thay vì sinh ra đoạn kỳ quặc")
    void shouldRejectInvalidParameters() {
        assertThatThrownBy(() -> TextChunker.chunk("abc", 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TextChunker.chunk("abc", 100, 100))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TextChunker.chunk("abc", 100, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
