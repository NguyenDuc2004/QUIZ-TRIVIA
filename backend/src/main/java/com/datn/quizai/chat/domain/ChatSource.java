package com.datn.quizai.chat.domain;

import java.util.UUID;

/**
 * Một đoạn học liệu đã được dùng để trả lời, lưu trong cột JSONB {@code chat_messages.sources}.
 *
 * @param excerpt đoạn văn bản đã cấp cho mô hình, cắt ngắn — để người học tự đối chiếu xem câu trả
 *                lời có thật bám tài liệu hay không. Không có nó thì "có trích dẫn" chỉ là cái nhãn
 */
public record ChatSource(UUID materialId, String title, String excerpt) {
}
