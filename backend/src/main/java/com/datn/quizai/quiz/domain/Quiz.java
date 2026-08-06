package com.datn.quizai.quiz.domain;

import com.datn.quizai.common.BaseEntity;
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
import org.hibernate.annotations.Formula;

import java.util.ArrayList;
import java.util.List;

/** Bài quiz — bảng `quizzes` (docs/database.md §1.2). */
@Entity
@Table(name = "quizzes")
@Getter
@Setter
@NoArgsConstructor
public class Quiz extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Difficulty difficulty = Difficulty.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Visibility visibility = Visibility.PRIVATE;

    @Column(name = "is_ai_generated", nullable = false)
    private boolean aiGenerated = false;

    /** Đường dẫn ảnh bìa do server sinh ({@code /uploads/images/…}); null = giao diện tự vẽ khối màu. */
    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    /** Thời gian làm bài toàn quiz (giây); null = không giới hạn. */
    @Column(name = "time_limit_sec")
    private Integer timeLimitSec;

    /** Danh sách câu hỏi kèm thứ tự trong quiz này. */
    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<QuizQuestion> quizQuestions = new ArrayList<>();

    /**
     * Số câu hỏi, tính ngay trong câu SELECT của quiz nên danh sách nhiều quiz
     * không sinh N+1 query. Chỉ đọc.
     */
    @Formula("(select count(*) from quiz_questions qq where qq.quiz_id = id)")
    private int questionCount;

    public Quiz(User owner, String title) {
        this.owner = owner;
        this.title = title;
    }
}
