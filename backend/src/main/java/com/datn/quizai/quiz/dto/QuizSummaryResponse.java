package com.datn.quizai.quiz.dto;

import com.datn.quizai.quiz.domain.Difficulty;
import com.datn.quizai.quiz.domain.Quiz;
import com.datn.quizai.quiz.domain.Visibility;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Quiz ở dạng tóm tắt cho danh sách và cho trang giới thiệu.
 * <b>Không</b> chứa câu hỏi — Guest xem được DTO này (docs/features/01-auth.md).
 */
public record QuizSummaryResponse(
        UUID id,
        String title,
        String description,
        UUID categoryId,
        String categoryName,
        Difficulty difficulty,
        Visibility visibility,
        boolean aiGenerated,
        String thumbnailUrl,
        Integer timeLimitSec,
        int questionCount,
        UUID ownerId,
        String ownerDisplayName,
        OffsetDateTime createdAt
) {
    /** Dùng cho danh sách: số câu hỏi lấy từ {@code @Formula} (đã tính trong câu SELECT). */
    public static QuizSummaryResponse from(Quiz quiz) {
        return from(quiz, quiz.getQuestionCount());
    }

    /**
     * Dùng khi vừa thay đổi danh sách câu hỏi trong cùng transaction: giá trị
     * {@code @Formula} khi đó vẫn là số cũ (tính lúc nạp entity), nên phải truyền số đếm thật.
     */
    public static QuizSummaryResponse from(Quiz quiz, int questionCount) {
        return new QuizSummaryResponse(
                quiz.getId(),
                quiz.getTitle(),
                quiz.getDescription(),
                quiz.getCategory() == null ? null : quiz.getCategory().getId(),
                quiz.getCategory() == null ? null : quiz.getCategory().getName(),
                quiz.getDifficulty(),
                quiz.getVisibility(),
                quiz.isAiGenerated(),
                quiz.getThumbnailUrl(),
                quiz.getTimeLimitSec(),
                questionCount,
                quiz.getOwner().getId(),
                quiz.getOwner().getDisplayName(),
                quiz.getCreatedAt());
    }
}
