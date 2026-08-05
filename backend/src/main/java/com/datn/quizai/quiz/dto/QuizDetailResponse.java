package com.datn.quizai.quiz.dto;

import com.datn.quizai.quiz.domain.Quiz;

import java.util.List;

/**
 * Chi tiết quiz kèm câu hỏi (có đáp án đúng) — chỉ dành cho chủ sở hữu/Admin,
 * dùng ở màn hình soạn quiz.
 */
public record QuizDetailResponse(
        QuizSummaryResponse quiz,
        List<QuestionResponse> questions
) {
    public static QuizDetailResponse from(Quiz quiz) {
        List<QuestionResponse> questions = quiz.getQuizQuestions().stream()
                .sorted((a, b) -> Integer.compare(a.getOrderIndex(), b.getOrderIndex()))
                .map(qq -> QuestionResponse.from(qq.getQuestion()))
                .toList();

        // Đếm từ danh sách đã nạp thay vì @Formula, để số câu đúng ngay sau khi vừa sửa danh sách
        return new QuizDetailResponse(QuizSummaryResponse.from(quiz, questions.size()), questions);
    }
}
