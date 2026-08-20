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
        /** FR-48 — người học cần biết TRƯỚC khi bấm bắt đầu, xem QuizIntroPage. */
        boolean strictExam,
        int questionCount,
        /**
         * Số NGƯỜI đã làm xong quiz này (không phải số lượt).
         * <p>
         * {@code 0} nghĩa là <b>chưa ai kịp làm</b>, không phải "quiz này dở" — giao diện phải ẩn hẳn con
         * số thay vì hiện "0 người đã làm", vì số 0 đọc như một lời chê và phạt oan mọi quiz mới.
         */
        int learnerCount,
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
                quiz.isStrictExam(),
                questionCount,
                quiz.getLearnerCount(),
                quiz.getOwner().getId(),
                quiz.getOwner().getDisplayName(),
                quiz.getCreatedAt());
    }
}
