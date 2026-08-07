package com.datn.quizai.ai.grading;

import com.datn.quizai.ai.AiJson;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Đọc và <b>kiểm duyệt</b> JSON chấm điểm do mô hình trả về (docs/features/06).
 * <p>
 * Điểm số là thứ trực tiếp đi vào học bạ của người học, nên không có chỗ cho "chắc mô hình trả
 * đúng". Ba thứ bắt buộc phải chặn:
 * <ol>
 *   <li><b>Điểm ngoài khoảng.</b> Mô hình vẫn trả 10 cho câu tối đa 5 điểm, hoặc trả điểm âm.
 *       Ép về [0, maxScore] — đây là hàng rào cuối cùng nếu prompt injection lọt qua và mô hình
 *       bị dụ cho điểm tối đa: nó vẫn không vượt được trần thật của câu.</li>
 *   <li><b>Thiếu điểm.</b> Không có {@code score} thì không đoán bừa — ném lỗi để câu đó vào trạng
 *       thái chấm hỏng và Creator chấm tay, còn hơn ghi một con số bịa vào bài của người học.</li>
 *   <li><b>{@code isCorrect} mâu thuẫn với điểm.</b> Mô hình hay trả {@code isCorrect: true} kèm
 *       điểm 3/10. Điểm là nguồn sự thật; cờ đúng/sai được suy lại từ điểm.</li>
 * </ol>
 * Lớp thuần logic, test trực tiếp được.
 */
public final class GradeJsonParser {

    private static final Logger log = LoggerFactory.getLogger(GradeJsonParser.class);

    /** Nhận xét quá dài thì cắt — cột TEXT chứa được nhưng giao diện thì không. */
    private static final int MAX_TEXT_LENGTH = 1500;

    private GradeJsonParser() {
    }

    /**
     * @param maxScore trần điểm của câu, lấy từ {@code attempt_answers.max_score} (đã chốt lúc bắt
     *                 đầu bài) chứ không lấy lại từ câu hỏi — Creator có thể đã sửa điểm sau đó
     * @throws IllegalStateException khi JSON không đọc được hoặc thiếu điểm
     */
    public static AiGrade parse(String rawJson, int maxScore) {
        JsonNode root = AiJson.read(rawJson);

        // Có mô hình bọc thêm một lớp {"result": {...}} hoặc {"grading": {...}}
        if (!root.has("score") && !root.has("diem")) {
            for (String wrapper : new String[]{"result", "grading", "data"}) {
                if (root.path(wrapper).isObject()) {
                    root = root.path(wrapper);
                    break;
                }
            }
        }

        Integer raw = AiJson.integer(root, "score", "diem", "points");
        if (raw == null) {
            throw new IllegalStateException("Mô hình không trả về điểm số");
        }

        int score = Math.clamp(raw, 0, maxScore);
        if (score != raw) {
            log.warn("Mô hình chấm {} điểm cho câu tối đa {} — đã ép về {}", raw, maxScore, score);
        }

        // Điểm là nguồn sự thật, cờ đúng/sai chỉ là cách hiển thị. Coi là "đúng" khi đạt trọn điểm;
        // đạt một phần thì không phải đúng, nhưng cũng không phải sai hẳn — giao diện hiển thị điểm.
        boolean correct = maxScore > 0 && score >= maxScore;

        return new AiGrade(
                score,
                correct,
                shorten(AiJson.text(root, "feedback", "nhanXet", "comment")),
                shorten(AiJson.text(root, "suggestions", "goiY", "suggestion", "improvement")));
    }

    /** Đọc phần giải thích đáp án; trả chuỗi rỗng nếu mô hình không nói gì. */
    public static String parseExplanation(String rawJson) {
        JsonNode root = AiJson.read(rawJson);
        return shorten(AiJson.text(root, "explanation", "giaiThich", "text"));
    }

    private static String shorten(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String trimmed = text.trim();
        return trimmed.length() <= MAX_TEXT_LENGTH ? trimmed : trimmed.substring(0, MAX_TEXT_LENGTH) + "…";
    }
}
