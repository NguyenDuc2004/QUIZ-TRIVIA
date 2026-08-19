package com.datn.quizai.classroom.repository;

import com.datn.quizai.classroom.domain.ClassroomMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClassroomMemberRepository extends JpaRepository<ClassroomMember, UUID> {

    Optional<ClassroomMember> findByClassroomIdAndUserId(UUID classroomId, UUID userId);

    /**
     * Thành viên của một lớp, kèm sẵn thông tin người dùng.
     * <p>
     * {@code join fetch} vì danh sách này luôn hiện tên và email: thiếu nó là N+1 lượt đi vòng tới cơ sở dữ
     * liệu cho một lớp 40 học sinh, và nó sẽ không lộ ra khi thử với lớp ba người.
     */
    @Query("""
            select m from ClassroomMember m
              join fetch m.user u
            where m.classroom.id = :classroomId
            order by m.role, u.displayName
            """)
    List<ClassroomMember> findByClassroomWithUser(@Param("classroomId") UUID classroomId);

    long countByClassroomId(UUID classroomId);
}
