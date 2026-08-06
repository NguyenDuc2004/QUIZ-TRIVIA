package com.datn.quizai.ai.rag;

import java.util.ArrayList;
import java.util.List;

/**
 * Cắt văn bản dài thành các đoạn có chồng lấn, chuẩn bị cho embedding
 * (docs/features/05 — pipeline ingestion).
 * <p>
 * <b>Vì sao phải chồng lấn:</b> cắt cứng theo độ dài rất dễ chặt ngang một ý — nửa đầu định nghĩa
 * nằm cuối đoạn này, nửa sau nằm đầu đoạn kia, và không đoạn nào trả lời trọn câu hỏi. Cho hai
 * đoạn liền nhau chia sẻ một phần đuôi/đầu thì ý bị chặt vẫn còn nguyên ở ít nhất một đoạn.
 * <p>
 * <b>Vì sao cắt theo câu:</b> cắt giữa chừng một câu tạo ra đoạn cụt nghĩa, embedding của nó
 * gần như vô dụng khi tìm kiếm. Nên chỉ cắt ở ranh giới câu, trừ khi gặp câu dài bất thường.
 * <p>
 * Lớp thuần logic, không phụ thuộc Spring — test trực tiếp được.
 */
public final class TextChunker {

    /** Khoảng 1500 ký tự ~ 400 token, đủ ngữ cảnh mà không nuốt hết cửa sổ prompt. */
    public static final int DEFAULT_CHUNK_SIZE = 1500;
    public static final int DEFAULT_OVERLAP = 200;
    /** Đoạn ngắn hơn mức này thường là rác (số trang, tiêu đề lẻ) nên bỏ. */
    private static final int MIN_CHUNK_LENGTH = 50;

    private TextChunker() {
    }

    public static List<String> chunk(String text) {
        return chunk(text, DEFAULT_CHUNK_SIZE, DEFAULT_OVERLAP);
    }

    public static List<String> chunk(String text, int chunkSize, int overlap) {
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize phải > 0");
        }
        if (overlap < 0 || overlap >= chunkSize) {
            throw new IllegalArgumentException("overlap phải nằm trong khoảng [0, chunkSize)");
        }

        String normalized = normalize(text);
        if (normalized.length() < MIN_CHUNK_LENGTH) {
            return normalized.isBlank() ? List.of() : List.of(normalized);
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;

        while (start < normalized.length()) {
            int end = Math.min(start + chunkSize, normalized.length());

            // Chưa tới cuối văn bản thì lùi về ranh giới câu gần nhất
            if (end < normalized.length()) {
                int boundary = lastSentenceEnd(normalized, start, end);
                if (boundary > start) {
                    end = boundary;
                }
            }

            String chunk = normalized.substring(start, end).trim();
            if (chunk.length() >= MIN_CHUNK_LENGTH) {
                chunks.add(chunk);
            }

            if (end >= normalized.length()) {
                break;
            }
            // Lùi lại `overlap` ký tự cho đoạn sau, nhưng luôn phải tiến ít nhất 1 ký tự
            start = Math.max(start + 1, end - overlap);
        }

        return chunks;
    }

    /**
     * Vị trí kết thúc câu cuối cùng trong khoảng [start, end).
     *
     * @return chỉ số ngay sau dấu chấm câu, hoặc {@code -1} nếu cả khoảng không có dấu nào
     *         (câu quá dài — khi đó cứ cắt cứng còn hơn dồn tất cả vào một đoạn khổng lồ)
     */
    private static int lastSentenceEnd(String text, int start, int end) {
        for (int i = end - 1; i > start; i--) {
            char c = text.charAt(i);
            if ((c == '.' || c == '!' || c == '?' || c == '\n')
                    && i + 1 < text.length() && Character.isWhitespace(text.charAt(i + 1))) {
                return i + 1;
            }
        }
        return -1;
    }

    /**
     * Gộp khoảng trắng thừa. PDF trích ra thường đầy dấu xuống dòng giữa câu và khoảng trắng
     * lặp; để nguyên thì đoạn nào cũng phí chỗ cho ký tự vô nghĩa.
     */
    private static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\r\n", "\n")
                .replaceAll("[ \\t\\x0B\\f]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }
}
