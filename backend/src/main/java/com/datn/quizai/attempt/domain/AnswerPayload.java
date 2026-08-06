package com.datn.quizai.attempt.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.UUID;

/**
 * Nội dung người dùng trả lời, lưu vào cột JSONB {@code attempt_answers.user_answer}.
 * <p>
 * Chỉ một trong hai trường có giá trị, tùy loại câu hỏi:
 * <ul>
 *   <li>{@code optionIds} — SINGLE_CHOICE / MULTIPLE_CHOICE / TRUE_FALSE</li>
 *   <li>{@code text} — FILL_BLANK / SHORT_ANSWER</li>
 * </ul>
 * Dùng JSONB thay vì cột riêng cho từng loại để thêm loại câu hỏi mới không phải đổi schema.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AnswerPayload(List<UUID> optionIds, String text) {

    public static AnswerPayload ofOptions(List<UUID> optionIds) {
        return new AnswerPayload(optionIds, null);
    }

    public static AnswerPayload ofText(String text) {
        return new AnswerPayload(null, text);
    }

    /**
     * {@code @JsonIgnore} là bắt buộc: không có nó, Jackson coi đây là thuộc tính {@code empty},
     * ghi thêm vào JSONB rồi lần đọc sau lại không nhận ra nó — Hibernate ném
     * {@code Could not deserialize string to java type}.
     */
    @JsonIgnore
    public boolean isEmpty() {
        return (optionIds == null || optionIds.isEmpty()) && (text == null || text.isBlank());
    }
}
