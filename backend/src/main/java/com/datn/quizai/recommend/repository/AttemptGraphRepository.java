package com.datn.quizai.recommend.repository;

import com.datn.quizai.attempt.domain.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Đọc dữ liệu từ PostgreSQL để dựng đồ thị gợi ý (docs/features/07).
 * <p>
 * Tách khỏi {@code QuizAttemptRepository} vì đây là truy vấn <b>phân tích</b>, gộp theo chủ đề chứ
 * không nạp thực thể để sửa. Trộn chung thì khó nhìn ra chỗ nào là nghiệp vụ làm bài, chỗ nào là
 * chuẩn bị dữ liệu cho Neo4j.
 * <p>
 * <b>Gộp ở CSDL chứ không gộp ở Java:</b> một người làm nhiều bài trên nhiều chủ đề, kéo hết
 * {@code attempt_answers} về rồi cộng trong bộ nhớ là chở dữ liệu đi vòng vô ích.
 */
public interface AttemptGraphRepository extends JpaRepository<QuizAttempt, UUID> {

    /** Thông tin một bài đã nộp, đủ để dựng cạnh {@code (User)-[:ATTEMPTED]->(Quiz)}. */
    @Query("""
            select a.user.id as userId,
                   a.quiz.id as quizId,
                   a.quiz.title as quizTitle,
                   a.quiz.visibility as visibility,
                   a.totalScore as totalScore,
                   a.maxScore as maxScore,
                   a.submittedAt as submittedAt
            from QuizAttempt a
            where a.id = :attemptId
            """)
    AttemptRow findAttemptRow(@Param("attemptId") UUID attemptId);

    /**
     * Chủ đề mà quiz này phủ, kèm số câu mỗi chủ đề — dựng cạnh {@code (Quiz)-[:COVERS]->(Topic)}.
     * <p>
     * Lấy từ {@code questions.topic} chứ không từ {@code quizzes.category_id}: một quiz trộn câu
     * nhiều chủ đề thì nó phủ tất cả, và đó mới là thứ dùng để gợi ý.
     */
    @Query("""
            select q.topic as topic, count(qq) as questionCount
            from QuizQuestion qq join qq.question q
            where qq.quiz.id = :quizId and q.topic is not null and q.topic <> ''
            group by q.topic
            """)
    List<TopicCountRow> findQuizTopics(@Param("quizId") UUID quizId);

    /**
     * Năng lực của người dùng trên từng chủ đề, gộp trên <b>toàn bộ</b> bài đã nộp — dựng cạnh
     * {@code (User)-[:PRACTICED]->(Topic)}.
     * <p>
     * Tính lại từ đầu mỗi lần đồng bộ thay vì cộng dồn: cộng dồn thì một lần đồng bộ chạy hai lần
     * là số liệu nhân đôi, mà bước đồng bộ <i>cố ý</i> chạy hai lần cho mỗi bài (một lần lúc nộp,
     * một lần sau khi AI chấm xong câu tự luận).
     * <p>
     * Chỉ tính câu <b>đã chấm xong</b>: câu đang chờ AI mà tính là sai thì người học bỗng "yếu"
     * ở chủ đề đó trong vài phút, rồi lại hết yếu — gợi ý nhảy loạn.
     */
    @Query("""
            select q.topic as topic,
                   sum(case when ans.correct = true then 1 else 0 end) as correctCount,
                   count(ans) as totalCount
            from AttemptAnswer ans
              join ans.question q
              join ans.attempt a
            where a.user.id = :userId
              and a.status <> com.datn.quizai.attempt.domain.AttemptStatus.IN_PROGRESS
              and q.topic is not null and q.topic <> ''
              and ans.gradedBy in (com.datn.quizai.attempt.domain.GradedBy.AUTO,
                                   com.datn.quizai.attempt.domain.GradedBy.AI,
                                   com.datn.quizai.attempt.domain.GradedBy.HUMAN)
            group by q.topic
            """)
    List<TopicMasteryRow> findUserTopicMastery(@Param("userId") UUID userId);

    /** Id mọi bài đã kết thúc của một người — dùng khi dựng lại đồ thị từ lịch sử. */
    @Query("""
            select a.id from QuizAttempt a
            where a.user.id = :userId
              and a.status <> com.datn.quizai.attempt.domain.AttemptStatus.IN_PROGRESS
            order by a.submittedAt
            """)
    List<UUID> findFinishedAttemptIds(@Param("userId") UUID userId);

    /** Một dòng bài làm đã nộp. */
    interface AttemptRow {
        UUID getUserId();

        UUID getQuizId();

        String getQuizTitle();

        com.datn.quizai.quiz.domain.Visibility getVisibility();

        int getTotalScore();

        int getMaxScore();

        java.time.OffsetDateTime getSubmittedAt();
    }

    /** Số câu của một quiz thuộc một chủ đề. */
    interface TopicCountRow {
        String getTopic();

        long getQuestionCount();
    }

    /**
     * Năng lực của người dùng trên một chủ đề.
     * <p>
     * Chỉ mang <b>số đếm thô</b>, không mang kết luận "yếu" hay "khá": ngưỡng nào là yếu nằm ở
     * truy vấn gợi ý, không nướng vào dữ liệu. Đổi ngưỡng thì không phải dựng lại đồ thị.
     */
    interface TopicMasteryRow {
        String getTopic();

        long getCorrectCount();

        long getTotalCount();
    }
}
