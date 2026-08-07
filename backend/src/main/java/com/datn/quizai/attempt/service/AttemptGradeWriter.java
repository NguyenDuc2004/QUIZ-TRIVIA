package com.datn.quizai.attempt.service;

import com.datn.quizai.ai.grading.AiGrade;
import com.datn.quizai.attempt.domain.AttemptAnswer;
import com.datn.quizai.attempt.domain.GradedBy;
import com.datn.quizai.attempt.domain.QuizAttempt;
import com.datn.quizai.attempt.repository.AttemptAnswerRepository;
import com.datn.quizai.attempt.repository.QuizAttemptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Ghi kết quả chấm xuống CSDL trong transaction ngắn, tách khỏi {@link AiGradingService}.
 * <p>
 * Phải là <b>bean riêng</b>: gọi {@code this.method()} trong cùng một lớp đi thẳng, không qua proxy
 * Spring, nên {@code @Transactional} mất tác dụng và điểm không bao giờ được ghi. Đây là cái bẫy đã
 * làm job sinh đề kẹt ở PENDING trước đây, nên lặp lại đúng khuôn {@code MaterialStatusWriter}.
 * <p>
 * Cũng không bọc cả mẻ chấm trong một transaction: mỗi câu là một lời gọi mô hình mất vài giây,
 * giữ transaction suốt thời gian đó là giam một kết nối CSDL vô ích.
 */
@Service
public class AttemptGradeWriter {

    private static final Logger log = LoggerFactory.getLogger(AttemptGradeWriter.class);

    private final AttemptAnswerRepository answerRepository;
    private final QuizAttemptRepository attemptRepository;

    public AttemptGradeWriter(AttemptAnswerRepository answerRepository,
                              QuizAttemptRepository attemptRepository) {
        this.answerRepository = answerRepository;
        this.attemptRepository = attemptRepository;
    }

    /**
     * Ghi điểm AI cho một câu.
     * <p>
     * Chỉ ghi khi câu <b>vẫn còn</b> ở {@code PENDING_AI}. Trong lúc mô hình đang chấm, Creator có
     * thể đã chấm tay câu đó rồi; ghi đè lên điểm người chấm bằng điểm máy là sai hướng — người
     * luôn thắng máy.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void applyAiGrade(UUID answerId, AiGrade grade) {
        answerRepository.findById(answerId).ifPresent(answer -> {
            if (!answer.isAwaitingAi()) {
                log.debug("Câu {} đã được chấm bằng cách khác, bỏ qua điểm AI", answerId);
                return;
            }
            answer.setScore(grade.score());
            answer.setCorrect(grade.correct());
            answer.setAiFeedback(emptyToNull(grade.feedback()));
            answer.setAiSuggestions(emptyToNull(grade.suggestions()));
            answer.setGradedBy(GradedBy.AI);
            answer.setGradedAt(OffsetDateTime.now());
        });
    }

    /**
     * Đánh dấu chấm hỏng. Cần một trạng thái dừng riêng, nếu không câu đó nằm mãi ở
     * {@code PENDING_AI} và người học thấy "đang chấm" vĩnh viễn.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAiFailed(UUID answerId, String reason) {
        answerRepository.findById(answerId).ifPresent(answer -> {
            if (!answer.isAwaitingAi()) {
                return;
            }
            answer.setGradedBy(GradedBy.AI_FAILED);
            answer.setAiFeedback("Chưa chấm tự động được. " + reason);
            answer.setGradedAt(OffsetDateTime.now());
        });
    }

    /**
     * Cộng lại tổng điểm của bài sau khi chấm xong.
     * <p>
     * Bắt buộc phải có: lúc nộp, câu tự luận còn 0 điểm nên {@code total_score} là điểm tạm. Không
     * cộng lại thì bảng xếp hạng và lịch sử giữ mãi con số thiếu, dù từng câu đã có điểm đúng.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recalculateTotal(UUID attemptId) {
        attemptRepository.findByIdWithAnswers(attemptId).ifPresent(attempt -> {
            int total = attempt.getAnswers().stream().mapToInt(AttemptAnswer::getScore).sum();
            if (total != attempt.getTotalScore()) {
                log.info("Chấm xong bài {}: {} → {} điểm", attemptId, attempt.getTotalScore(), total);
                attempt.setTotalScore(total);
            }
        });
    }

    /** Chủ quiz chấm tay, ghi đè điểm AI (docs/features/06 §Use case). */
    @Transactional
    public void applyHumanGrade(QuizAttempt attempt, AttemptAnswer answer, int score, String feedback) {
        answer.setScore(Math.clamp(score, 0, answer.getMaxScore()));
        answer.setCorrect(answer.getMaxScore() > 0 && answer.getScore() >= answer.getMaxScore());
        answer.setAiFeedback(emptyToNull(feedback));
        answer.setGradedBy(GradedBy.HUMAN);
        answer.setGradedAt(OffsetDateTime.now());

        attempt.setTotalScore(attempt.getAnswers().stream().mapToInt(AttemptAnswer::getScore).sum());
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
