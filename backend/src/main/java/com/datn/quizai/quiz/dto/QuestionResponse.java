package com.datn.quizai.quiz.dto;

import com.datn.quizai.quiz.domain.Difficulty;
import com.datn.quizai.quiz.domain.Question;
import com.datn.quizai.quiz.domain.QuestionSource;
import com.datn.quizai.quiz.domain.QuestionType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Câu hỏi kèm đáp án đúng — chỉ trả cho <b>chủ sở hữu</b> hoặc Admin.
 * Khi người học làm bài, đáp án đúng bị lược bỏ (xử lý ở features/03 gameplay).
 */
public record QuestionResponse(
        UUID id,
        QuestionType type,
        String content,
        String explanation,
        Difficulty difficulty,
        String topic,
        Integer points,
        Integer timeLimitSec,
        QuestionSource source,
        List<OptionResponse> options,
        OffsetDateTime createdAt
) {
    public record OptionResponse(UUID id, String content, boolean correct, Integer orderIndex) {
    }

    public static QuestionResponse from(Question question) {
        List<OptionResponse> options = question.getOptions().stream()
                .sorted((a, b) -> Integer.compare(a.getOrderIndex(), b.getOrderIndex()))
                .map(o -> new OptionResponse(o.getId(), o.getContent(), o.isCorrect(), o.getOrderIndex()))
                .toList();

        return new QuestionResponse(
                question.getId(),
                question.getType(),
                question.getContent(),
                question.getExplanation(),
                question.getDifficulty(),
                question.getTopic(),
                question.getPoints(),
                question.getTimeLimitSec(),
                question.getSource(),
                options,
                question.getCreatedAt());
    }
}
