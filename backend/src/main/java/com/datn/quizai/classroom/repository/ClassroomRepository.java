package com.datn.quizai.classroom.repository;

import com.datn.quizai.classroom.domain.Classroom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClassroomRepository extends JpaRepository<Classroom, UUID> {

    Optional<Classroom> findByClassCode(String classCode);

    boolean existsByClassCode(String classCode);

    /** Lớp tôi làm chủ nhiệm. */
    List<Classroom> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

    /**
     * Lớp tôi tham gia với tư cách thành viên (học sinh hoặc trợ giảng).
     * <p>
     * Tách khỏi danh sách lớp mình làm chủ thay vì gộp một truy vấn {@code union}: giao diện hiện hai nhóm
     * riêng vì hai vai trò làm hai việc khác nhau, và gộp lại thì lại phải tách ra ở tầng trên.
     */
    @Query("""
            select m.classroom from ClassroomMember m
            where m.user.id = :userId
            order by m.joinedAt desc
            """)
    List<Classroom> findJoinedBy(@Param("userId") UUID userId);
}
