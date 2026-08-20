package com.datn.quizai.attempt.dto;

import com.datn.quizai.attempt.domain.AnswerPayload;
import com.datn.quizai.attempt.domain.AttemptAnswer;
import com.datn.quizai.attempt.domain.GradedBy;
import com.datn.quizai.quiz.domain.Difficulty;
import com.datn.quizai.quiz.domain.Question;
import com.datn.quizai.quiz.domain.QuestionOption;
import com.datn.quizai.quiz.domain.QuestionType;

import java.util.List;
import java.util.UUID;

/**
 * Một câu hỏi trong bài làm.
 * <p>
 * <b>Quy tắc bảo mật:</b> khi bài chưa nộp, mọi trường lộ đáp án ({@code correctOptionIds},
 * {@code explanation}, {@code correct}, {@code score}) đều là null — người làm bài không có
 * cách nào đọc được đáp án từ response. Chỉ sau khi nộp (hoặc ở chế độ luyện tập, với riêng
 * câu vừa trả lời) các trường đó mới có giá trị.
 */
public record AttemptQuestionResponse(
        /** Id dòng câu trả lời — Creator cần nó để ghi đè điểm (features/06). */
        UUID answerId,
        UUID questionId,
        int orderIndex,
        QuestionType type,
        String content,
        /**
         * Ảnh minh hoạ của câu hỏi (features/02, FR-11); null = câu hỏi chỉ có chữ.
         * <p>
         * Ảnh nằm ở <b>phần đề bài</b> nên luôn hiện, kể cả lúc chưa nộp — khác hẳn {@code explanation} và
         * {@code correctOptionIds} vốn chỉ lộ sau khi nộp. Thiếu trường này thì ảnh lưu được nhưng người
         * học không bao giờ thấy, và cả tính năng thành vô nghĩa.
         */
        String imageUrl,
        Difficulty difficulty,
        int maxScore,
        Integer timeLimitSec,
        List<OptionView> options,
        AnswerPayload userAnswer,
        // --- chỉ có giá trị khi được phép lộ đáp án ---
        List<UUID> correctOptionIds,
        String explanation,
        Boolean correct,
        Integer score,
        GradedBy gradedBy,
        String aiFeedback,
        String aiSuggestions
) {
    /** Lựa chọn hiển thị cho người làm bài — <b>không</b> mang cờ đúng/sai. */
    public record OptionView(UUID id, String content) {
    }

    /** Dạng đang làm bài: giấu toàn bộ đáp án. */
    public static AttemptQuestionResponse hidden(AttemptAnswer answer) {
        return build(answer, false);
    }

    /** Dạng xem kết quả: kèm đáp án đúng, giải thích và điểm từng câu (FR-17). */
    public static AttemptQuestionResponse revealed(AttemptAnswer answer) {
        return build(answer, true);
    }

    private static AttemptQuestionResponse build(AttemptAnswer answer, boolean reveal) {
        Question question = answer.getQuestion();

        // Câu điền khuyết/tự luận lưu đáp án được chấp nhận ngay trong options, nên lúc đang
        // làm bài phải giấu hẳn — chỉ câu trắc nghiệm mới cần hiện lựa chọn để người dùng chọn.
        // Khi đã nộp thì hiện tất cả để người học đối chiếu đáp án đúng (FR-17).
        boolean showOptions = question.getType().isChoiceBased() || reveal;
        List<OptionView> options = showOptions
                ? question.getOptions().stream()
                        .sorted((a, b) -> Integer.compare(a.getOrderIndex(), b.getOrderIndex()))
                        .map(o -> new OptionView(o.getId(), o.getContent()))
                        .toList()
                : List.of();

        return new AttemptQuestionResponse(
                answer.getId(),
                question.getId(),
                answer.getOrderIndex(),
                question.getType(),
                question.getContent(),
                question.getImageUrl(),
                question.getDifficulty(),
                answer.getMaxScore(),
                question.getTimeLimitSec(),
                options,
                answer.getUserAnswer(),
                reveal ? correctIds(question) : null,
                reveal ? question.getExplanation() : null,
                reveal ? answer.getCorrect() : null,
                reveal ? answer.getScore() : null,
                reveal ? answer.getGradedBy() : null,
                reveal ? answer.getAiFeedback() : null,
                reveal ? answer.getAiSuggestions() : null);
    }

    /**
     * Id các lựa chọn đúng. Với câu điền khuyết đây là những đáp án được chấp nhận,
     * với câu tự luận là đáp án mẫu — cả hai chỉ trả về sau khi nộp bài.
     */
    private static List<UUID> correctIds(Question question) {
        return question.getOptions().stream()
                .filter(QuestionOption::isCorrect)
                .map(QuestionOption::getId)
                .toList();
    }
}
