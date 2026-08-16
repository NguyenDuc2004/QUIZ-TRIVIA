package com.datn.quizai.ai.generation;

import com.datn.quizai.ai.AiJson;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Đọc và <b>lọc</b> danh sách thẻ do mô hình trả về (features/11, FR-38).
 * <p>
 * Lọc chứ không chỉ đọc. Mô hình trả JSON đúng cú pháp vẫn có thể trả thẻ vô dụng, và mỗi thẻ rác lọt vào
 * bộ là một lần người học phải ôn một thứ không đáng ôn — rồi lịch SRS còn mang nó quay lại nhiều lần nữa.
 * Ba loại bị loại ở đây, mỗi loại là một cách hỏng đã thấy thật khi chạy thử:
 * <ol>
 *   <li><b>Thiếu mặt trước hoặc mặt sau</b> — thẻ một mặt không ôn được.</li>
 *   <li><b>Mặt sau quá dài</b> — mô hình có xu hướng giảng bài. Thẻ mà mặt sau là một đoạn văn thì người
 *       học không tự đối chiếu nổi trong vài giây, tức mất đúng cái làm nên flashcard.</li>
 *   <li><b>Trùng mặt trước</b> — hỏi lại cùng một điều bằng chữ khác. Lịch SRS sẽ nhân đôi công ôn cho
 *       cùng một kiến thức.</li>
 * </ol>
 * Danh sách bị loại được trả về kèm lý do, không im lặng bỏ đi: người dùng cần biết vì sao yêu cầu 15 thẻ
 * mà chỉ nhận được 11.
 */
public final class FlashcardJsonParser {

    private static final Logger log = LoggerFactory.getLogger(FlashcardJsonParser.class);

    /**
     * Giới hạn độ dài mặt sau.
     * <p>
     * 400 ký tự là khoảng ba câu tiếng Việt — đã dài hơn mức prompt yêu cầu (tối đa 2 câu), nên đây là
     * lưới chắn cho trường hợp mô hình phớt lờ chỉ dẫn, không phải chuẩn mực. Cắt ngắn thay vì loại bỏ là
     * lựa chọn tệ hơn: câu bị cắt giữa dòng làm mặt sau sai nghĩa.
     */
    static final int MAX_BACK_LENGTH = 400;

    static final int MAX_FRONT_LENGTH = 300;

    private FlashcardJsonParser() {
    }

    public record ParseResult(List<GeneratedFlashcard> flashcards, List<String> rejected) {
    }

    public static ParseResult parse(String rawJson) {
        JsonNode root = AiJson.read(rawJson);
        JsonNode array = root.isArray() ? root : root.path("flashcards");

        if (!array.isArray()) {
            throw new IllegalStateException("Kết quả không phải danh sách thẻ");
        }

        List<GeneratedFlashcard> accepted = new ArrayList<>();
        List<String> rejected = new ArrayList<>();
        Set<String> daThay = new HashSet<>();

        for (JsonNode node : array) {
            String front = AiJson.text(node, "front", "mặt trước", "question", "term").trim();
            String back = AiJson.text(node, "back", "mặt sau", "answer", "definition").trim();
            String hint = AiJson.text(node, "hint", "gợi ý").trim();

            if (front.isBlank() || back.isBlank()) {
                rejected.add("Thiếu mặt trước hoặc mặt sau: " + shorten(front.isBlank() ? back : front));
                continue;
            }
            if (front.length() > MAX_FRONT_LENGTH) {
                rejected.add("Mặt trước quá dài (" + front.length() + " ký tự): " + shorten(front));
                continue;
            }
            if (back.length() > MAX_BACK_LENGTH) {
                rejected.add("Mặt sau quá dài (" + back.length() + " ký tự), không ôn nhanh được: "
                        + shorten(front));
                continue;
            }
            // Chuẩn hoá về chữ thường và bỏ khoảng trắng lặp trước khi so: mô hình hay trả hai thẻ khác
            // nhau đúng một dấu câu hoặc một khoảng trắng.
            String dauVet = front.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
            if (!daThay.add(dauVet)) {
                rejected.add("Trùng mặt trước: " + shorten(front));
                continue;
            }

            accepted.add(new GeneratedFlashcard(front, back, hint.isBlank() ? null : hint));
        }

        if (!rejected.isEmpty()) {
            log.info("Sinh thẻ: nhận {} thẻ, loại {} thẻ ({})", accepted.size(), rejected.size(), rejected);
        }
        return new ParseResult(accepted, rejected);
    }

    private static String shorten(String text) {
        String gon = text.replaceAll("\\s+", " ").trim();
        return gon.length() <= 60 ? gon : gon.substring(0, 60) + "…";
    }
}
