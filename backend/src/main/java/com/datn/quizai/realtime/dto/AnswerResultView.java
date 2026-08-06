package com.datn.quizai.realtime.dto;

import java.util.UUID;

/**
 * Kết quả câu vừa trả lời, gửi <b>riêng</b> cho người trả lời qua
 * {@code /user/queue/room/{code}}.
 * <p>
 * Không phát cho cả phòng: người trả lời nhanh mà biết mình đúng thì người còn lại
 * chỉ cần nhìn phản ứng cũng đoán được đáp án.
 *
 * @param elapsedMillis thời gian server đo được, để client giải thích vì sao điểm cao/thấp
 */
public record AnswerResultView(
        UUID questionId,
        boolean correct,
        int points,
        int totalScore,
        long elapsedMillis
) {
}
