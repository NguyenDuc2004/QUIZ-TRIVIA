package com.datn.quizai.classroom.domain;

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

/** Một lớp học (features/14, FR-54). */
@Entity
@Table(name = "classrooms")
@Getter
@Setter
@NoArgsConstructor
public class Classroom extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false, updatable = false)
    private User owner;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    /** Mã 6 ký tự học sinh gõ để vào lớp. Không đổi sau khi tạo — đã phát cho cả lớp rồi. */
    @Column(name = "class_code", nullable = false, length = 6, updatable = false)
    private String classCode;

    public Classroom(User owner, String name, String description, String classCode) {
        this.owner = owner;
        this.name = name;
        this.description = description;
        this.classCode = classCode;
    }
}
