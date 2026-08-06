package com.datn.quizai.attempt.domain;

import com.datn.quizai.common.BaseEntity;
import com.datn.quizai.quiz.domain.Quiz;
import com.datn.quizai.user.domain.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Một lượt làm bài — bảng `quiz_attempts` (docs/features/03-gameplay.md).
 * <p>
 * Đề bài được <b>chốt ngay lúc bắt đầu</b>: mọi câu hỏi của quiz được sao thành các dòng
 * {@link AttemptAnswer}. Nhờ vậy chủ quiz sửa danh sách câu hỏi giữa chừng cũng không làm
 * hỏng bài đang làm hay lệch điểm bài đã nộp.
 */
@Entity
@Table(name = "quiz_attempts")
@Getter
@Setter
@NoArgsConstructor
public class QuizAttempt extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AttemptMode mode = AttemptMode.EXAM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private AttemptStatus status = AttemptStatus.IN_PROGRESS;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt = OffsetDateTime.now();

    /** Hạn nộp; null khi quiz không giới hạn thời gian. */
    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    @Column(name = "total_score", nullable = false)
    private int totalScore = 0;

    @Column(name = "max_score", nullable = false)
    private int maxScore = 0;

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<AttemptAnswer> answers = new ArrayList<>();

    public QuizAttempt(User user, Quiz quiz, AttemptMode mode) {
        this.user = user;
        this.quiz = quiz;
        this.mode = mode;
    }

    public void addAnswer(AttemptAnswer answer) {
        answer.setAttempt(this);
        answers.add(answer);
    }

    /** Đã quá hạn nộp hay chưa (chỉ có nghĩa khi quiz có giới hạn thời gian). */
    public boolean isExpiredAt(OffsetDateTime now) {
        return expiresAt != null && now.isAfter(expiresAt);
    }
}
