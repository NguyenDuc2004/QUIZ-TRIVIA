package com.datn.quizai.attempt.domain;

import com.datn.quizai.quiz.domain.Question;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Một câu trong đề của một lượt làm bài — bảng `attempt_answers`.
 * <p>
 * Dòng được tạo sẵn ngay khi bắt đầu làm bài (đề đã chốt), lúc đó {@code userAnswer} còn null.
 * Người dùng trả lời thì ghi đè {@code userAnswer}; điểm chỉ được ghi khi chấm.
 */
@Entity
@Table(name = "attempt_answers")
@Getter
@Setter
@NoArgsConstructor
public class AttemptAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false)
    private QuizAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    /** Thứ tự câu trong đề, sao lại từ `quiz_questions.order_index` lúc bắt đầu. */
    @Column(name = "order_index", nullable = false)
    private Integer orderIndex = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "user_answer")
    private AnswerPayload userAnswer;

    /** null = chưa chấm; với câu tự luận chờ AI cũng để null cho tới khi có kết quả. */
    @Column(name = "is_correct")
    private Boolean correct;

    @Column(nullable = false)
    private int score = 0;

    /** Điểm tối đa của câu, chốt lúc bắt đầu để chủ quiz sửa điểm sau không lệch bài cũ. */
    @Column(name = "max_score", nullable = false)
    private int maxScore = 1;

    /** Nhận xét về bài đã làm (features/06). */
    @Column(name = "ai_feedback", columnDefinition = "text")
    private String aiFeedback;

    /** Việc cần làm để khá hơn — tách khỏi nhận xét để giao diện nhấn mạnh riêng. */
    @Column(name = "ai_suggestions", columnDefinition = "text")
    private String aiSuggestions;

    @Enumerated(EnumType.STRING)
    @Column(name = "graded_by", nullable = false, length = 15)
    private GradedBy gradedBy = GradedBy.NOT_GRADED;

    @Column(name = "answered_at")
    private OffsetDateTime answeredAt;

    /** Chấm xong lúc nào — để dò câu kẹt {@code PENDING_AI} quá lâu. */
    @Column(name = "graded_at")
    private OffsetDateTime gradedAt;

    public AttemptAnswer(Question question, int orderIndex) {
        this.question = question;
        this.orderIndex = orderIndex;
        this.maxScore = question.getPoints() == null ? 1 : question.getPoints();
    }

    public boolean isAnswered() {
        return userAnswer != null && !userAnswer.isEmpty();
    }

    /** Câu đã nộp nhưng AI chưa chấm xong — người học đang thấy "đang chấm". */
    public boolean isAwaitingAi() {
        return gradedBy == GradedBy.PENDING_AI;
    }
}
