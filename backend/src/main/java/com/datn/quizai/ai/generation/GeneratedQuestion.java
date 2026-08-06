package com.datn.quizai.ai.generation;

import com.datn.quizai.quiz.domain.Difficulty;
import com.datn.quizai.quiz.domain.QuestionType;

import java.util.List;

/**
 * Một câu hỏi <b>nháp</b> do AI sinh ra, đã qua validate nhưng <b>chưa lưu vào ngân hàng</b>.
 * <p>
 * Human-in-the-loop là yêu cầu bắt buộc của features/05: AI có thể bịa, nên Creator phải xem và
 * duyệt trước khi câu hỏi thành đề thật. DTO này tồn tại đúng cho khoảng giữa đó.
 *
 * @param sourceExcerpt đoạn học liệu mà câu hỏi dựa vào — để Creator đối chiếu xem AI có bịa không
 */
public record GeneratedQuestion(
        QuestionType type,
        String content,
        List<Option> options,
        String explanation,
        Difficulty difficulty,
        String topic,
        String sourceExcerpt
) {
    public record Option(String content, boolean correct) {
    }
}
