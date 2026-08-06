package com.datn.quizai.attempt.dto;

import com.datn.quizai.attempt.domain.AttemptMode;
import com.datn.quizai.attempt.domain.AttemptStatus;
import com.datn.quizai.attempt.domain.QuizAttempt;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Tóm tắt một lượt làm bài — dùng cho danh sách lịch sử (FR-18) và cho phần đầu trang kết quả.
 * Không chứa câu hỏi nên nhẹ và không lộ nội dung đề.
 */
public record AttemptSummaryResponse(
        UUID id,
        UUID quizId,
        String quizTitle,
        AttemptMode mode,
        AttemptStatus status,
        OffsetDateTime startedAt,
        OffsetDateTime expiresAt,
        OffsetDateTime submittedAt,
        int totalScore,
        int maxScore,
        int questionCount,
        int answeredCount,
        int correctCount,
        Integer durationSec
) {
    /** Số câu / số câu đã trả lời / số câu đúng của một bài làm. */
    public record Counts(int questionCount, int answeredCount, int correctCount) {
        public static final Counts ZERO = new Counts(0, 0, 0);
    }

    /** Dùng khi collection {@code answers} đã được nạp sẵn (trang kết quả, trang làm bài). */
    public static AttemptSummaryResponse from(QuizAttempt attempt) {
        return of(attempt, new Counts(
                attempt.getAnswers().size(),
                (int) attempt.getAnswers().stream().filter(a -> a.isAnswered()).count(),
                (int) attempt.getAnswers().stream().filter(a -> Boolean.TRUE.equals(a.getCorrect())).count()));
    }

    /**
     * Dùng cho danh sách lịch sử: số đếm lấy từ một câu truy vấn gộp riêng thay vì nạp
     * {@code answers} của từng bài, tránh N+1 khi liệt kê nhiều bài.
     */
    public static AttemptSummaryResponse of(QuizAttempt attempt, Counts counts) {
        return new AttemptSummaryResponse(
                attempt.getId(),
                attempt.getQuiz().getId(),
                attempt.getQuiz().getTitle(),
                attempt.getMode(),
                attempt.getStatus(),
                attempt.getStartedAt(),
                attempt.getExpiresAt(),
                attempt.getSubmittedAt(),
                attempt.getTotalScore(),
                attempt.getMaxScore(),
                counts.questionCount(),
                counts.answeredCount(),
                counts.correctCount(),
                durationSec(attempt));
    }

    /** Thời gian làm bài thực tế; null khi bài chưa nộp. */
    private static Integer durationSec(QuizAttempt attempt) {
        if (attempt.getSubmittedAt() == null) {
            return null;
        }
        return (int) Duration.between(attempt.getStartedAt(), attempt.getSubmittedAt()).toSeconds();
    }
}
