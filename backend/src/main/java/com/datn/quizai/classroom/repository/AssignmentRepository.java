package com.datn.quizai.classroom.repository;

import com.datn.quizai.classroom.domain.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {

    @Query("""
            select a from Assignment a
              join fetch a.quiz q
            where a.classroom.id = :classroomId
            order by a.createdAt desc
            """)
    List<Assignment> findByClassroomWithQuiz(@Param("classroomId") UUID classroomId);

    /**
     * Bài tập của <b>mọi lớp tôi tham gia</b> — màn "Bài tập của tôi" của học sinh.
     * <p>
     * Chỉ trả bài <b>đã tới giờ mở</b>: bài hẹn giờ mở tuần sau mà hiện ra ngay hôm nay thì học sinh bấm vào
     * và nhận lỗi, hoặc tệ hơn là tưởng mình đã bỏ lỡ.
     */
    @Query("""
            select a from Assignment a
              join fetch a.quiz q
              join fetch a.classroom c
            where a.classroom.id in (
                    select m.classroom.id from ClassroomMember m where m.user.id = :userId)
              and (a.openAt is null or a.openAt <= :now)
            order by case when a.dueAt is null then 1 else 0 end, a.dueAt, a.createdAt desc
            """)
    List<Assignment> findVisibleFor(@Param("userId") UUID userId, @Param("now") OffsetDateTime now);

    /**
     * Bài tập sắp hết hạn trong một khoảng — nguồn của job nhắc hạn nộp (features/16).
     * <p>
     * Trả về cả lớp để tầng trên biết phải nhắc những ai mà không phải truy vấn thêm cho từng bài.
     */
    @Query("""
            select a from Assignment a
              join fetch a.classroom c
            where a.dueAt is not null
              and a.dueAt between :tu and :den
            """)
    List<Assignment> findDueBetween(@Param("tu") OffsetDateTime tu, @Param("den") OffsetDateTime den);

    boolean existsByQuizId(UUID quizId);
}
