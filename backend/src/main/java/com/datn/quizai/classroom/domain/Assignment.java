package com.datn.quizai.classroom.domain;

import com.datn.quizai.common.BaseEntity;
import com.datn.quizai.quiz.domain.Quiz;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/** Một bài tập giao cho lớp (features/14, FR-55). */
@Entity
@Table(name = "assignments")
@Getter
@Setter
@NoArgsConstructor
public class Assignment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "classroom_id", nullable = false, updatable = false)
    private Classroom classroom;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false, updatable = false)
    private Quiz quiz;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "text")
    private String instruction;

    /** null = mở ngay khi giao. */
    @Column(name = "open_at")
    private OffsetDateTime openAt;

    /** null = không có hạn nộp. */
    @Column(name = "due_at")
    private OffsetDateTime dueAt;

    public Assignment(Classroom classroom, Quiz quiz, String title, String instruction,
                      OffsetDateTime openAt, OffsetDateTime dueAt) {
        this.classroom = classroom;
        this.quiz = quiz;
        this.title = title;
        this.instruction = instruction;
        this.openAt = openAt;
        this.dueAt = dueAt;
    }

    /** Đã tới giờ mở chưa. Không có {@code openAt} nghĩa là mở ngay. */
    public boolean daMo(OffsetDateTime now) {
        return openAt == null || !now.isBefore(openAt);
    }

    /** Đã quá hạn chưa. Không có {@code dueAt} nghĩa là không bao giờ quá hạn. */
    public boolean quaHan(OffsetDateTime now) {
        return dueAt != null && now.isAfter(dueAt);
    }
}
