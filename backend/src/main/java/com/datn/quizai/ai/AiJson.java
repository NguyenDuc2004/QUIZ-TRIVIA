package com.datn.quizai.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Đọc JSON do mô hình ngôn ngữ trả về.
 * <p>
 * Tách riêng vì mọi tính năng AI đều vấp cùng hai chuyện, bất kể yêu cầu "chỉ trả JSON thuần":
 * mô hình bọc kết quả trong khối <code>```json</code>, và mỗi mô hình đặt tên trường một kiểu
 * ({@code score} / {@code diem}, {@code feedback} / {@code comment}). Xử lý ở một chỗ để lớp sinh
 * đề và lớp chấm bài không phải chép lại cùng đoạn code — và khi gặp kiểu trả lời quái lạ mới thì
 * chỉ sửa một nơi.
 */
public final class AiJson {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AiJson() {
    }

    /** Bỏ khối ```…``` bọc ngoài nếu có. */
    public static String stripCodeFence(String raw) {
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

    /** @throws IllegalStateException khi chuỗi không phải JSON đọc được */
    public static JsonNode readTree(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Mô hình trả về JSON không đọc được", e);
        }
    }

    /** Bỏ khối mã rồi đọc — dạng dùng nhiều nhất. */
    public static JsonNode read(String raw) {
        return readTree(stripCodeFence(raw));
    }

    /** Lấy giá trị của trường đầu tiên có mặt — mỗi mô hình đặt tên một kiểu. */
    public static String text(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.path(field);
            if (value.isTextual()) {
                return value.asText();
            }
        }
        return "";
    }

    /**
     * Lấy số nguyên của trường đầu tiên có mặt. Chấp nhận cả số ghi dưới dạng chuỗi ({@code "7"})
     * và số thực ({@code 7.5} → 7) vì mô hình trả cả ba kiểu.
     */
    public static Integer integer(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.path(field);
            if (value.isNumber()) {
                return value.asInt();
            }
            if (value.isTextual()) {
                try {
                    return (int) Double.parseDouble(value.asText().trim().replace(',', '.'));
                } catch (NumberFormatException ignored) {
                    // thử trường kế tiếp
                }
            }
        }
        return null;
    }
}
