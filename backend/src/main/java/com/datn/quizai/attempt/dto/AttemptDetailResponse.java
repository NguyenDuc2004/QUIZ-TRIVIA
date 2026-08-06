package com.datn.quizai.attempt.dto;

import com.datn.quizai.attempt.domain.QuizAttempt;

import java.util.List;

/**
 * Toàn bộ một lượt làm bài: phần tóm tắt + danh sách câu hỏi theo thứ tự đề.
 * <p>
 * Cùng một DTO dùng cho hai màn hình, khác nhau ở chỗ câu hỏi có lộ đáp án hay không:
 * <ul>
 *   <li>bài <b>chưa nộp</b> → {@link AttemptQuestionResponse#hidden} (giấu đáp án)</li>
 *   <li>bài <b>đã nộp</b> → {@link AttemptQuestionResponse#revealed} (kèm đáp án đúng, giải thích, điểm)</li>
 * </ul>
 */
public record AttemptDetailResponse(
        AttemptSummaryResponse attempt,
        List<AttemptQuestionResponse> questions
) {
    public static AttemptDetailResponse from(QuizAttempt attempt) {
        boolean reveal = attempt.getStatus().isFinished();

        List<AttemptQuestionResponse> questions = attempt.getAnswers().stream()
                .sorted((a, b) -> Integer.compare(a.getOrderIndex(), b.getOrderIndex()))
                .map(answer -> reveal
                        ? AttemptQuestionResponse.revealed(answer)
                        : AttemptQuestionResponse.hidden(answer))
                .toList();

        return new AttemptDetailResponse(AttemptSummaryResponse.from(attempt), questions);
    }
}
