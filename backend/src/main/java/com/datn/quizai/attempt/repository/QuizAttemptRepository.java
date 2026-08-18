package com.datn.quizai.attempt.repository;

import com.datn.quizai.attempt.domain.AttemptStatus;
import com.datn.quizai.attempt.domain.QuizAttempt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, UUID> {

    /** Bài đang làm dở của người dùng trên một quiz — gọi lại API bắt đầu thì làm tiếp bài này. */
    @Query("""
            select a from QuizAttempt a
              join fetch a.quiz q
              join fetch q.owner
            where a.user.id = :userId and q.id = :quizId and a.status = :status
            """)
    Optional<QuizAttempt> findByUserAndQuizAndStatus(@Param("userId") UUID userId,
                                                     @Param("quizId") UUID quizId,
                                                     @Param("status") AttemptStatus status);

    /**
     * Nạp bài làm kèm toàn bộ câu hỏi trong đề.
     * <p>
     * Chỉ fetch một collection kiểu List ({@code answers}); lựa chọn của câu hỏi để Hibernate
     * nạp theo lô nhờ {@code @BatchSize} trên {@code Question.options} — fetch hai List cùng lúc
     * sẽ lỗi MultipleBagFetchException.
     */
    @Query("""
            select distinct a from QuizAttempt a
              join fetch a.quiz q
              join fetch q.owner
              left join fetch a.answers ans
              left join fetch ans.question
            where a.id = :id
            """)
    Optional<QuizAttempt> findByIdWithAnswers(@Param("id") UUID id);

    /** Lịch sử làm bài (FR-18), mới nhất trước. */
    @Query(value = """
            select a from QuizAttempt a
              join fetch a.quiz q
              left join fetch q.category
            where a.user.id = :userId
              and (:quizId is null or q.id = :quizId)
            order by a.startedAt desc
            """,
            countQuery = "select count(a) from QuizAttempt a where a.user.id = :userId and (:quizId is null or a.quiz.id = :quizId)")
    Page<QuizAttempt> findHistory(@Param("userId") UUID userId,
                                  @Param("quizId") UUID quizId,
                                  Pageable pageable);

    /**
     * Đếm số câu / số câu đã trả lời / số câu đúng cho nhiều bài làm trong <b>một</b> truy vấn.
     * Dùng cho danh sách lịch sử để khỏi nạp collection {@code answers} của từng bài (N+1).
     */
    @Query("""
            select ans.attempt.id as attemptId,
                   count(ans) as questionCount,
                   sum(case when ans.answeredAt is not null then 1 else 0 end) as answeredCount,
                   sum(case when ans.correct = true then 1 else 0 end) as correctCount
            from AttemptAnswer ans
            where ans.attempt.id in :attemptIds
            group by ans.attempt.id
            """)
    List<AttemptCountRow> countAnswersByAttemptIds(@Param("attemptIds") List<UUID> attemptIds);

    /** Projection cho {@link #countAnswersByAttemptIds}. */
    interface AttemptCountRow {
        UUID getAttemptId();

        long getQuestionCount();

        long getAnsweredCount();

        long getCorrectCount();
    }

    /**
     * Bảng xếp hạng một quiz (FR-19): mỗi người chỉ lấy <b>một</b> bài tốt nhất —
     * điểm cao nhất, đồng điểm thì bài nộp sớm hơn xếp trên.
     * <p>
     * <b>Loại bài của chính chủ quiz</b> ({@code a.user_id <> q.owner_id}): họ soạn đề nên biết
     * trước đáp án, để lên bảng thì cuộc đua mất công bằng. Chủ quiz vẫn làm bài và xem điểm của
     * mình trong lịch sử bình thường.
     * <p>
     * Viết bằng SQL thuần vì JPQL không có {@code distinct on} của PostgreSQL.
     */
    @Query(value = """
            select distinct on (a.user_id)
                   a.id, a.user_id, u.display_name, a.total_score, a.max_score,
                   a.started_at, a.submitted_at
            from quiz_attempts a
                     join users u on u.id = a.user_id
                     join quizzes q on q.id = a.quiz_id
            where a.quiz_id = :quizId
              and a.status in ('SUBMITTED', 'EXPIRED')
              and a.user_id <> q.owner_id
            order by a.user_id, a.total_score desc, a.submitted_at asc
            """, nativeQuery = true)
    List<LeaderboardRow> findBestAttemptPerUser(@Param("quizId") UUID quizId);

    /**
     * Projection cho câu truy vấn bảng xếp hạng ở trên.
     * <p>
     * Cột {@code timestamptz} qua native query về dưới dạng {@link Instant} — khai báo
     * {@code OffsetDateTime} ở đây sẽ lỗi <i>Cannot project java.time.Instant</i> lúc chạy,
     * nên service tự đổi múi giờ khi dựng DTO.
     */
    interface LeaderboardRow {
        UUID getUserId();

        String getDisplayName();

        Integer getTotalScore();

        Integer getMaxScore();

        Instant getStartedAt();

        Instant getSubmittedAt();
    }
    // ==================================================== features/14 — bài tập được giao

    /** Mọi lượt của một bài tập, kèm sẵn người làm — nguồn của bảng theo dõi lớp (FR-57). */
    @Query("""
            select a from QuizAttempt a
              join fetch a.user u
            where a.assignmentId = :assignmentId
            """)
    List<QuizAttempt> findByAssignmentId(@Param("assignmentId") UUID assignmentId);

    Optional<QuizAttempt> findByAssignmentIdAndUserId(UUID assignmentId, UUID userId);

    /**
     * Mọi lượt bài-tập của một người.
     * <p>
     * Một truy vấn cho cả màn "Bài tập của tôi", thay vì hỏi riêng cho từng bài tập. Học sinh học năm lớp
     * thì màn đó có vài chục bài, và hỏi từng cái là vài chục lượt đi vòng tới cơ sở dữ liệu.
     */
    @Query("select a from QuizAttempt a where a.user.id = :userId and a.assignmentId is not null")
    List<QuizAttempt> findByUserWithAssignment(@Param("userId") UUID userId);
}
