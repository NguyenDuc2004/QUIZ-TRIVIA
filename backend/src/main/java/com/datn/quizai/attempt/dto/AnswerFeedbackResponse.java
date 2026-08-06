package com.datn.quizai.attempt.dto;

import com.datn.quizai.attempt.domain.AnswerPayload;

import java.util.List;
import java.util.UUID;

/**
 * Kết quả của một lần gửi câu trả lời.
 * <p>
 * Ở chế độ <b>thi</b> chỉ xác nhận đã lưu — {@code correct}, {@code correctOptionIds},
 * {@code explanation} đều null để không lộ đáp án giữa chừng.
 * Ở chế độ <b>luyện tập</b> chấm ngay câu đó và trả về đáp án + giải thích (FR-14).
 *
 * @param answeredCount số câu đã trả lời / tổng số câu, để client cập nhật thanh tiến độ
 */
public record AnswerFeedbackResponse(
        UUID questionId,
        AnswerPayload userAnswer,
        int answeredCount,
        int questionCount,
        Boolean correct,
        Integer score,
        List<UUID> correctOptionIds,
        String explanation
) {
    /** Chế độ thi: chỉ báo đã lưu. */
    public static AnswerFeedbackResponse saved(UUID questionId, AnswerPayload userAnswer,
                                               int answeredCount, int questionCount) {
        return new AnswerFeedbackResponse(questionId, userAnswer, answeredCount, questionCount,
                null, null, null, null);
    }
}
