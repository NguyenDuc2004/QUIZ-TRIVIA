package com.datn.quizai.attempt.service;

import com.datn.quizai.ai.grading.AiGrade;
import com.datn.quizai.ai.grading.GradeJsonParser;
import com.datn.quizai.ai.grading.GradingPromptBuilder;
import com.datn.quizai.ai.provider.AiCompletion;
import com.datn.quizai.ai.provider.AiOrchestrator;
import com.datn.quizai.ai.provider.AiPrompt;
import com.datn.quizai.attempt.domain.AttemptAnswer;
import com.datn.quizai.attempt.repository.AttemptAnswerRepository;
import com.datn.quizai.quiz.domain.Question;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.UUID;

/**
 * Chấm câu tự luận bằng AI (docs/features/06 — FR-30).
 * <p>
 * <b>Chạy nền, sau khi transaction nộp bài đã commit.</b> Hai lý do:
 * <ol>
 *   <li>Mỗi câu là một lời gọi mô hình mất vài giây. Chấm đồng bộ thì người học bấm "Nộp bài" rồi
 *       ngồi nhìn màn hình quay hàng chục giây với bài nhiều câu tự luận — và request nào cũng có
 *       thể timeout giữa chừng, để lại bài nộp dở.</li>
 *   <li>{@code @TransactionalEventListener} với pha AFTER_COMMIT: khởi động luồng nền ngay lúc gọi
 *       thì nó đọc CSDL trước khi dòng bài làm kịp commit và không thấy câu nào cần chấm.</li>
 * </ol>
 * Người học nhận kết quả ngay với điểm phần trắc nghiệm, phần tự luận hiện "đang chấm" rồi tự cập
 * nhật — thay vì chờ tất cả.
 * <p>
 * <b>Điểm tối đa lấy từ {@code attempt_answers.max_score}</b>, không lấy lại từ câu hỏi: điểm đã
 * chốt lúc bắt đầu bài, Creator sửa điểm câu hỏi sau đó cũng không được làm lệch bài cũ.
 */
@Service
public class AiGradingService {

    private static final Logger log = LoggerFactory.getLogger(AiGradingService.class);

    /** Tên tính năng ghi vào bảng audit `ai_request_logs`. */
    private static final String FEATURE = "grade-answer";
    private static final String FEATURE_EXPLAIN = "explain-answer";

    /** Chấm cần bám tiêu chí, không cần sáng tạo — để mô hình tự do là điểm số thất thường. */
    private static final double TEMPERATURE = 0.1;

    private final AttemptAnswerRepository answerRepository;
    private final AiOrchestrator aiOrchestrator;
    private final AttemptGradeWriter gradeWriter;

    public AiGradingService(AttemptAnswerRepository answerRepository,
                            AiOrchestrator aiOrchestrator,
                            AttemptGradeWriter gradeWriter) {
        this.answerRepository = answerRepository;
        this.aiOrchestrator = aiOrchestrator;
        this.gradeWriter = gradeWriter;
    }

    /**
     * Nuốt mọi lỗi thay vì để văng ra: đây là luồng nền, không ai đứng đó nhận exception. Lỗi của
     * một câu không được làm hỏng những câu còn lại — mỗi câu tự chịu trách nhiệm cho mình.
     */
    @Async("aiTaskExecutor")
    @TransactionalEventListener
    public void onAttemptSubmitted(AttemptSubmittedEvent event) {
        try {
            gradePendingAnswers(event.attemptId(), event.userId());
        } catch (Exception e) {
            log.error("Chấm bài {} thất bại", event.attemptId(), e);
        }
    }

    /** Tách riêng để test gọi thẳng được, không phải dựng cả cơ chế sự kiện. */
    public void gradePendingAnswers(UUID attemptId, UUID userId) {
        List<AttemptAnswer> pending = loadPending(attemptId);
        if (pending.isEmpty()) {
            return;
        }

        log.info("Bắt đầu chấm {} câu tự luận của bài {}", pending.size(), attemptId);
        for (AttemptAnswer answer : pending) {
            gradeOne(answer, userId);
        }
        gradeWriter.recalculateTotal(attemptId);
    }

    /**
     * Nạp danh sách câu chờ chấm trong một transaction ngắn rồi thoát ra ngay — phần gọi mô hình
     * kéo dài nằm ngoài, không giam kết nối CSDL.
     */
    @Transactional(readOnly = true)
    public List<AttemptAnswer> loadPending(UUID attemptId) {
        return answerRepository.findPendingAiByAttempt(attemptId);
    }

    private void gradeOne(AttemptAnswer answer, UUID userId) {
        Question question = answer.getQuestion();
        String userText = answer.getUserAnswer() == null ? null : answer.getUserAnswer().text();

        // Bỏ trống thì không tốn một lời gọi mô hình để biết là 0 điểm
        if (userText == null || userText.isBlank()) {
            gradeWriter.applyAiGrade(answer.getId(),
                    new AiGrade(0, false, "Câu này bỏ trống, không có nội dung để chấm.", ""));
            return;
        }

        try {
            AiPrompt prompt = new AiPrompt(
                    GradingPromptBuilder.systemInstruction(),
                    GradingPromptBuilder.userPrompt(question, userText, answer.getMaxScore()),
                    true,
                    TEMPERATURE);

            // background = true: chấm chạy nền, không ai ngồi đợi, nên chờ được đúng thời gian
            // Gemini đề nghị khi vượt hạn mức. Không có nó thì bài từ câu tự luận thứ sáu trở đi
            // đều rơi vào AI_FAILED trên gói miễn phí (5 lượt/phút).
            AiCompletion completion = aiOrchestrator.complete(prompt, FEATURE, userId, true);
            AiGrade grade = GradeJsonParser.parse(completion.text(), answer.getMaxScore());

            gradeWriter.applyAiGrade(answer.getId(), grade);
            log.debug("Chấm câu {}: {}/{} điểm", answer.getId(), grade.score(), answer.getMaxScore());

        } catch (Exception e) {
            log.warn("Không chấm được câu {}: {}", answer.getId(), e.getMessage());
            gradeWriter.markAiFailed(answer.getId(), shortReason(e));
        }
    }

    /**
     * Giải thích đáp án một câu (docs/features/06 §Ghi chú kỹ thuật).
     * <p>
     * Với câu có đáp án cố định, <b>chỉ</b> dùng AI để giải thích chứ không để chấm — chấm đã xong
     * bằng logic, gọi mô hình thêm chỉ tốn tiền mà không chính xác hơn.
     * <p>
     * Gọi đồng bộ vì đây là hành động người dùng chủ động bấm và đứng chờ, khác với chấm hàng loạt.
     */
    public String explain(Question question, String userAnswerText, UUID userId) {
        AiPrompt prompt = new AiPrompt(
                GradingPromptBuilder.explainSystemInstruction(),
                GradingPromptBuilder.explainUserPrompt(question, userAnswerText),
                true,
                0.3);

        AiCompletion completion = aiOrchestrator.complete(prompt, FEATURE_EXPLAIN, userId);
        return GradeJsonParser.parseExplanation(completion.text());
    }

    /** Thông điệp lỗi của bên thứ ba có thể rất dài; cắt cho vừa ô hiển thị. */
    private static String shortReason(Exception e) {
        String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        return message.length() <= 200 ? message : message.substring(0, 200) + "…";
    }
}
