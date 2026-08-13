package com.datn.quizai.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Một lượt hỏi trợ lý học tập (FR-31).
 *
 * @param sessionId  null = mở phiên mới. Client không tự sinh id phiên: server đặt tiêu đề từ câu
 *                   hỏi đầu tiên nên nó phải là nơi tạo phiên
 * @param materialId giới hạn câu trả lời trong một tài liệu; null = tìm trong mọi tài liệu người gọi
 *                   đọc được. Hữu ích khi ôn đúng một chương và không muốn lẫn tài liệu khác
 * @param question   giới hạn 2000 ký tự: dài hơn thì không còn là câu hỏi mà là dán cả tài liệu vào,
 *                   và học liệu đã có đường riêng để nạp
 */
public record ChatAskRequest(
        UUID sessionId,
        UUID materialId,
        @NotBlank(message = "Nhập câu hỏi")
        @Size(max = 2000, message = "Câu hỏi tối đa 2000 ký tự")
        String question
) {
}
