package com.datn.quizai.classroom.domain;

import com.datn.quizai.user.domain.User;
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
 * Một người trong lớp (features/14, FR-54).
 * <p>
 * Không kế thừa {@code BaseEntity} vì bảng cố ý không có {@code updated_at}: thứ duy nhất đổi được là
 * {@code role}, và nếu cần biết ai đổi lúc nào thì thứ đúng để thêm là một dòng nhật ký, không phải một mốc
 * thời gian không nói ai đổi.
 */
@Entity
@Table(name = "classroom_members")
@Getter
@Setter
@NoArgsConstructor
public class ClassroomMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "classroom_id", nullable = false, updatable = false)
    private Classroom classroom;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private MemberRole role = MemberRole.STUDENT;

    @CreationTimestamp
    @Column(name = "joined_at", nullable = false, updatable = false)
    private OffsetDateTime joinedAt;

    public ClassroomMember(Classroom classroom, User user) {
        this.classroom = classroom;
        this.user = user;
    }
}
