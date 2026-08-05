package com.datn.quizai.quiz.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.util.UUID;

/**
 * Lựa chọn / đáp án của câu hỏi — bảng `question_options`.
 * <p>
 * Ý nghĩa thay đổi theo {@link QuestionType}:
 * <ul>
 *   <li>SINGLE_CHOICE, MULTIPLE_CHOICE, TRUE_FALSE — một lựa chọn hiển thị cho người làm bài,
 *       {@code isCorrect} đánh dấu đáp án đúng.</li>
 *   <li>FILL_BLANK — một đáp án được chấp nhận (tất cả đều {@code isCorrect = true}).</li>
 *   <li>SHORT_ANSWER — đáp án mẫu duy nhất, làm căn cứ để AI chấm (features/06).</li>
 * </ul>
 * Không kế thừa {@code BaseEntity} vì bảng này không có mốc thời gian riêng.
 */
@Entity
@Table(name = "question_options")
@Getter
@Setter
@NoArgsConstructor
public class QuestionOption {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "is_correct", nullable = false)
    private boolean correct = false;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex = 0;

    public QuestionOption(String content, boolean correct) {
        this.content = content;
        this.correct = correct;
    }
}
