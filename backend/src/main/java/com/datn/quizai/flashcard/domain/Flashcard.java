package com.datn.quizai.flashcard.domain;

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
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Một thẻ ghi nhớ — bảng `flashcards` (features/11).
 * <p>
 * Không kế thừa {@code BaseEntity}: thẻ chỉ cần {@code created_at}. Trạng thái thay đổi theo thời gian
 * của việc ôn tập nằm ở {@link FlashcardReview} — <b>theo từng người dùng</b> — chứ không nằm ở đây, nên
 * bản thân thẻ gần như không bị sửa sau khi tạo.
 */
@Entity
@Table(name = "flashcards")
@Getter
@Setter
@NoArgsConstructor
public class Flashcard {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "deck_id", nullable = false)
    private FlashcardDeck deck;

    @Column(nullable = false, columnDefinition = "text")
    private String front;

    @Column(nullable = false, columnDefinition = "text")
    private String back;

    @Column(columnDefinition = "text")
    private String hint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FlashcardSource source = FlashcardSource.MANUAL;

    /**
     * Câu hỏi gốc khi thẻ sinh từ một câu trả lời sai; {@code null} với thẻ tự viết hoặc sinh từ học liệu.
     * Xoá câu hỏi thì cột này về null — thẻ vẫn còn giá trị ôn tập, chỉ mất đường truy về nguồn.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private Question question;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
