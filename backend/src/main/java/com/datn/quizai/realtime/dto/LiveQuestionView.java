package com.datn.quizai.realtime.dto;

import com.datn.quizai.quiz.domain.Question;
import com.datn.quizai.quiz.domain.QuestionOption;
import com.datn.quizai.quiz.domain.QuestionType;

import java.util.List;
import java.util.UUID;

/**
 * Câu hỏi phát cho người chơi trong phòng.
 * <p>
 * <b>Không chứa đáp án đúng</b> — chỉ id và nội dung lựa chọn. Đáp án đúng đi trong
 * {@link GameEventType#QUESTION_CLOSED}, sau khi câu đã đóng.
 *
 * @param deadlineAtMillis mốc hết giờ theo đồng hồ server; client đếm ngược tới mốc này
 */
public record LiveQuestionView(
        UUID questionId,
        int index,
        int total,
        QuestionType type,
        String content,
        /** Ảnh minh hoạ của câu hỏi (FR-11); null = câu chỉ có chữ. */
        String imageUrl,
        int points,
        int timeLimitSec,
        long deadlineAtMillis,
        List<Option> options
) {
    public record Option(UUID id, String content) {
    }

    public static LiveQuestionView of(Question question, int index, int total,
                                      int timeLimitSec, long deadlineAtMillis) {
        // Câu điền khuyết/tự luận lưu đáp án ngay trong options nên phải giấu, giống màn làm bài đơn
        List<Option> options = question.getType().isChoiceBased()
                ? question.getOptions().stream()
                        .sorted((a, b) -> Integer.compare(a.getOrderIndex(), b.getOrderIndex()))
                        .map(o -> new Option(o.getId(), o.getContent()))
                        .toList()
                : List.of();

        return new LiveQuestionView(
                question.getId(), index, total, question.getType(), question.getContent(),
                question.getImageUrl(),
                question.getPoints() == null ? 1 : question.getPoints(),
                timeLimitSec, deadlineAtMillis, options);
    }

    /** Id các lựa chọn đúng — chỉ dùng khi dựng sự kiện QUESTION_CLOSED. */
    public static List<UUID> correctOptionIds(Question question) {
        return question.getOptions().stream()
                .filter(QuestionOption::isCorrect)
                .map(QuestionOption::getId)
                .toList();
    }
}
