package com.datn.quizai.ai.generation;

import com.datn.quizai.quiz.domain.Difficulty;
import com.datn.quizai.quiz.domain.QuestionType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Đọc và <b>kiểm duyệt</b> JSON do mô hình trả về (docs/features/05 §Schema JSON đầu ra).
 * <p>
 * Đây là lớp phòng thủ quan trọng nhất của tính năng sinh đề. Mô hình ngôn ngữ có thể:
 * bọc JSON trong khối ```json, trả thiếu trường, đặt {@code type} sai tên, sinh câu trắc nghiệm
 * không có đáp án đúng nào, hoặc lặp lại cùng một câu nhiều lần. Nếu tin thẳng đầu ra thì những
 * thứ đó sẽ chui vào ngân hàng câu hỏi.
 * <p>
 * Nguyên tắc: <b>bỏ câu hỏng, giữ câu tốt</b>. Một câu sai không được làm hỏng cả mẻ — người dùng
 * đã chờ hàng chục giây, trả về 8 câu dùng được vẫn hơn báo lỗi toàn bộ.
 * <p>
 * Lớp thuần logic, test trực tiếp được.
 */
public final class QuestionJsonParser {

    private static final Logger log = LoggerFactory.getLogger(QuestionJsonParser.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private QuestionJsonParser() {
    }

    /** Kết quả đọc: các câu dùng được và lý do những câu bị loại. */
    public record ParseResult(List<GeneratedQuestion> questions, List<String> rejected) {
    }

    public static ParseResult parse(String rawJson) {
        JsonNode root = readTree(stripCodeFence(rawJson));

        JsonNode array = root.path("questions");
        if (!array.isArray()) {
            // Có mô hình trả thẳng mảng thay vì bọc trong {"questions": [...]}
            array = root.isArray() ? root : MAPPER.createArrayNode();
        }

        List<GeneratedQuestion> accepted = new ArrayList<>();
        List<String> rejected = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        for (JsonNode node : array) {
            try {
                GeneratedQuestion question = readQuestion(node);

                // Lọc trùng: mô hình rất hay diễn đạt lại cùng một ý
                String fingerprint = question.content().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
                if (!seen.add(fingerprint)) {
                    rejected.add("Trùng nội dung: " + shorten(question.content()));
                    continue;
                }
                accepted.add(question);

            } catch (IllegalArgumentException e) {
                rejected.add(e.getMessage());
            }
        }

        if (!rejected.isEmpty()) {
            log.info("Sinh đề: nhận {} câu, loại {} câu ({})", accepted.size(), rejected.size(), rejected);
        }
        return new ParseResult(accepted, rejected);
    }

    // ------------------------------------------------------------------ nội bộ

    private static GeneratedQuestion readQuestion(JsonNode node) {
        String content = text(node, "question", "content");
        if (content.isBlank()) {
            throw new IllegalArgumentException("Thiếu nội dung câu hỏi");
        }

        QuestionType type = parseType(text(node, "type"));
        List<GeneratedQuestion.Option> options = readOptions(node, type);
        validate(type, options, content);

        return new GeneratedQuestion(
                type,
                content.trim(),
                options,
                text(node, "explanation").trim(),
                parseDifficulty(text(node, "difficulty")),
                emptyToNull(text(node, "topic").trim()),
                emptyToNull(text(node, "sourceExcerpt", "source").trim()));
    }

    /**
     * Đọc lựa chọn. Mô hình thường trả {@code options} là mảng chuỗi kèm {@code correctAnswer}
     * riêng (đúng theo schema ở tài liệu), nhưng đôi khi trả mảng object {@code {content, correct}}.
     * Chấp nhận cả hai để không loại oan câu hỏi chỉ vì khác cách gói.
     */
    private static List<GeneratedQuestion.Option> readOptions(JsonNode node, QuestionType type) {
        List<GeneratedQuestion.Option> options = new ArrayList<>();
        JsonNode raw = node.path("options");

        if (type == QuestionType.FILL_BLANK || type == QuestionType.SHORT_ANSWER) {
            // Hai loại này không có lựa chọn; "đáp án" chính là các cách trả lời được chấp nhận
            for (JsonNode answer : answersOf(node)) {
                String value = answer.asText("").trim();
                if (!value.isBlank()) {
                    options.add(new GeneratedQuestion.Option(value, true));
                }
            }
            return options;
        }

        Set<String> correctAnswers = new LinkedHashSet<>();
        for (JsonNode answer : answersOf(node)) {
            correctAnswers.add(answer.asText("").trim().toLowerCase(Locale.ROOT));
        }

        for (JsonNode option : raw) {
            if (option.isObject()) {
                String value = text(option, "content", "text").trim();
                if (!value.isBlank()) {
                    options.add(new GeneratedQuestion.Option(value, option.path("correct").asBoolean(false)));
                }
            } else {
                String value = option.asText("").trim();
                if (!value.isBlank()) {
                    options.add(new GeneratedQuestion.Option(value,
                            correctAnswers.contains(value.toLowerCase(Locale.ROOT))));
                }
            }
        }
        return options;
    }

    /** {@code correctAnswer} có thể là chuỗi đơn hoặc mảng (câu nhiều đáp án). */
    private static List<JsonNode> answersOf(JsonNode node) {
        List<JsonNode> answers = new ArrayList<>();
        for (String field : List.of("correctAnswer", "correctAnswers", "answer", "answers")) {
            JsonNode value = node.path(field);
            if (value.isArray()) {
                value.forEach(answers::add);
            } else if (!value.isMissingNode() && !value.isNull()) {
                answers.add(value);
            }
        }
        return answers;
    }

    /**
     * Áp đúng luật của {@code QuestionService} — nếu không, câu hỏi qua được bước này rồi vẫn
     * bị chặn khi Creator bấm lưu, và họ không hiểu vì sao.
     */
    private static void validate(QuestionType type, List<GeneratedQuestion.Option> options, String content) {
        long correct = options.stream().filter(GeneratedQuestion.Option::correct).count();

        switch (type) {
            case SINGLE_CHOICE -> require(options.size() >= 2 && correct == 1,
                    "Câu một đáp án cần ≥2 lựa chọn và đúng 1 đáp án đúng", content);
            case MULTIPLE_CHOICE -> require(options.size() >= 3 && correct >= 2 && correct < options.size(),
                    "Câu nhiều đáp án cần ≥3 lựa chọn, ≥2 đáp án đúng và còn ≥1 lựa chọn sai", content);
            case TRUE_FALSE -> require(options.size() == 2 && correct == 1,
                    "Câu Đúng/Sai cần đúng 2 lựa chọn và 1 đáp án đúng", content);
            case FILL_BLANK -> require(!options.isEmpty(),
                    "Câu điền khuyết cần ít nhất 1 đáp án được chấp nhận", content);
            case SHORT_ANSWER -> require(options.size() == 1,
                    "Câu tự luận cần đúng 1 đáp án mẫu", content);
        }
    }

    private static void require(boolean condition, String reason, String content) {
        if (!condition) {
            throw new IllegalArgumentException(reason + " — " + shorten(content));
        }
    }

    private static QuestionType parseType(String raw) {
        String normalized = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        try {
            return QuestionType.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Loại câu hỏi không hợp lệ: " + raw);
        }
    }

    /** Độ khó sai/thiếu thì mặc định MEDIUM, không đáng loại cả câu hỏi vì chuyện này. */
    private static Difficulty parseDifficulty(String raw) {
        try {
            return Difficulty.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return Difficulty.MEDIUM;
        }
    }

    /**
     * Gỡ khối ```json ... ``` mà mô hình hay bọc quanh JSON dù đã yêu cầu trả JSON thuần.
     */
    private static String stripCodeFence(String raw) {
        String text = raw == null ? "" : raw.trim();
        if (!text.startsWith("```")) {
            return text;
        }
        int firstNewline = text.indexOf('\n');
        int lastFence = text.lastIndexOf("```");
        if (firstNewline < 0 || lastFence <= firstNewline) {
            return text;
        }
        return text.substring(firstNewline + 1, lastFence).trim();
    }

    private static JsonNode readTree(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Mô hình trả về JSON không đọc được", e);
        }
    }

    /** Lấy giá trị của trường đầu tiên có mặt — mỗi mô hình đặt tên một kiểu. */
    private static String text(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.path(field);
            if (value.isTextual()) {
                return value.asText();
            }
        }
        return "";
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String shorten(String text) {
        return text.length() <= 60 ? text : text.substring(0, 60) + "…";
    }
}
