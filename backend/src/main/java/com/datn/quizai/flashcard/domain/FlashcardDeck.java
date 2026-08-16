package com.datn.quizai.flashcard.domain;

import com.datn.quizai.common.BaseEntity;
import com.datn.quizai.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Một bộ thẻ ghi nhớ — bảng `flashcard_decks` (features/11). */
@Entity
@Table(name = "flashcard_decks")
@Getter
@Setter
@NoArgsConstructor
public class FlashcardDeck extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    /** Chủ đề tự do, cùng cách dùng như {@code questions.topic} để sau gộp được thống kê theo chủ đề. */
    @Column(length = 100)
    private String topic;
}
