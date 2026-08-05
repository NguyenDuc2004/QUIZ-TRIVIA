package com.datn.quizai.quiz.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Đặt lại toàn bộ danh sách câu hỏi của một quiz — <b>thứ tự trong mảng chính là
 * thứ tự câu hỏi</b> (`quiz_questions.order_index`). Gửi mảng rỗng để bỏ hết câu hỏi.
 * <p>
 * Thiết kế idempotent (thay thế cả danh sách) thay vì thêm/bớt từng câu để tránh
 * lệch thứ tự khi người dùng kéo-thả nhiều lần trên giao diện.
 */
public record SetQuizQuestionsRequest(

        @NotNull(message = "Danh sách câu hỏi không được null")
        List<UUID> questionIds
) {
}
