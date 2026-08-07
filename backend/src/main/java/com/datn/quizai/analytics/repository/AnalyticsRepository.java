package com.datn.quizai.analytics.repository;

import com.datn.quizai.attempt.domain.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Truy vấn tổng hợp cho thống kê (docs/features/09).
 * <p>
 * <b>Gộp ở CSDL, không gộp ở Java.</b> Thống kê chạm vào toàn bộ lịch sử làm bài; kéo hết
 * {@code attempt_answers} về rồi cộng trong bộ nhớ là chở dữ liệu đi vòng vô ích, và càng dùng lâu
 * càng chậm.
 * <p>
 * Tách khỏi {@code QuizAttemptRepository} vì đây là truy vấn <b>chỉ đọc để báo cáo</b>, không nạp
 * thực thể để sửa — trộn chung thì khó nhìn ra chỗ nào là nghiệp vụ làm bài.
 */
public interface AnalyticsRepository extends JpaRepository<QuizAttempt, UUID> {

    // ================================================================ Learner (FR-26)

    /**
     * Tổng quan của một người học.
     * <p>
     * {@code distinctQuizzes} khác {@code totalAttempts}: làm lại một quiz ba lần thì đó là ba lượt
     * nhưng vẫn một quiz. Hai con số nói hai chuyện khác nhau — "học được bao nhiêu" và "luyện bao
     * nhiêu lần" — nên trả cả hai.
     */
    @Query("""
            select count(a) as totalAttempts,
                   count(distinct a.quiz.id) as distinctQuizzes,
                   coalesce(sum(a.totalScore), 0) as sumScore,
                   coalesce(sum(a.maxScore), 0) as sumMaxScore
            from QuizAttempt a
            where a.user.id = :userId
              and a.status <> com.datn.quizai.attempt.domain.AttemptStatus.IN_PROGRESS
            """)
    LearnerOverviewRow findLearnerOverview(@Param("userId") UUID userId);

    /**
     * Điểm từng lượt theo thời gian — để vẽ đường tiến bộ.
     * <p>
     * Trả điểm thô và điểm tối đa chứ không trả sẵn phần trăm: quiz khác nhau có thang điểm khác
     * nhau, tự chia ở tầng trên thì chỗ làm tròn nằm một nơi duy nhất.
     */
    @Query("""
            select a.submittedAt as submittedAt,
                   a.quiz.title as quizTitle,
                   a.totalScore as score,
                   a.maxScore as maxScore
            from QuizAttempt a
            where a.user.id = :userId
              and a.status <> com.datn.quizai.attempt.domain.AttemptStatus.IN_PROGRESS
              and a.submittedAt is not null
            order by a.submittedAt
            """)
    List<AttemptScoreRow> findLearnerScoreTrend(@Param("userId") UUID userId);

    // ================================================================ Creator (FR-27)

    /**
     * Tổng quan một quiz: bao nhiêu lượt, bao nhiêu người, điểm trung bình, và bao nhiêu lượt
     * <b>nộp kịp giờ</b>.
     * <p>
     * "Tỉ lệ hoàn thành" mà FR-27 yêu cầu ở đây là tỉ lệ nộp kịp so với bị hết giờ (`EXPIRED`) —
     * con số nói lên đề có quá dài hay thời gian đặt quá ngắn. Không tính bài đang làm dở, vì bài
     * dở chưa nói được gì.
     */
    @Query("""
            select count(a) as totalAttempts,
                   count(distinct a.user.id) as distinctLearners,
                   coalesce(sum(a.totalScore), 0) as sumScore,
                   coalesce(sum(a.maxScore), 0) as sumMaxScore,
                   sum(case when a.status = com.datn.quizai.attempt.domain.AttemptStatus.SUBMITTED
                            then 1 else 0 end) as submittedCount
            from QuizAttempt a
            where a.quiz.id = :quizId
              and a.status <> com.datn.quizai.attempt.domain.AttemptStatus.IN_PROGRESS
            """)
    QuizOverviewRow findQuizOverview(@Param("quizId") UUID quizId);

    /**
     * Phân bố điểm theo mười khoảng 10% — dữ liệu cho biểu đồ cột.
     * <p>
     * Chia khoảng <b>ở CSDL</b> chứ không trả từng điểm về rồi chia ở client: số lượt có thể rất
     * lớn, mà thứ cần vẽ chỉ là mười con số.
     * <p>
     * Truy vấn native vì JPQL không có hàm chia khoảng; {@code least(...9)} để bài đạt điểm tối đa
     * rơi vào khoảng cuối chứ không tạo ra khoảng thứ mười một.
     */
    @Query(value = """
            select least(floor(a.total_score * 10.0 / nullif(a.max_score, 0)), 9) as bucket,
                   count(*) as attemptCount
            from quiz_attempts a
            where a.quiz_id = :quizId
              and a.status <> 'IN_PROGRESS'
              and a.max_score > 0
            group by 1
            order by 1
            """, nativeQuery = true)
    List<ScoreBucketRow> findScoreDistribution(@Param("quizId") UUID quizId);

    /**
     * Câu hỏi bị làm sai nhiều nhất — thứ Creator cần để biết đề chỗ nào có vấn đề.
     * <p>
     * Chỉ tính câu <b>đã chấm xong</b>: câu đang chờ AI chấm chưa có kết luận đúng/sai, tính nó là
     * sai thì câu tự luận nào cũng trông như câu khó nhất đề.
     * <p>
     * Trả cả số lượt trả lời để tầng trên tự quyết định bao nhiêu lượt mới đáng kết luận — một câu
     * sai 1/1 lượt không phải câu khó, chỉ là câu ít người làm.
     */
    @Query("""
            select q.id as questionId,
                   q.content as content,
                   q.topic as topic,
                   count(ans) as answeredCount,
                   sum(case when ans.correct = true then 0 else 1 end) as wrongCount
            from AttemptAnswer ans
              join ans.question q
              join ans.attempt a
            where a.quiz.id = :quizId
              and a.status <> com.datn.quizai.attempt.domain.AttemptStatus.IN_PROGRESS
              and ans.gradedBy in (com.datn.quizai.attempt.domain.GradedBy.AUTO,
                                   com.datn.quizai.attempt.domain.GradedBy.AI,
                                   com.datn.quizai.attempt.domain.GradedBy.HUMAN)
            group by q.id, q.content, q.topic
            order by sum(case when ans.correct = true then 0 else 1 end) desc, count(ans) desc
            """)
    List<HardQuestionRow> findHardestQuestions(@Param("quizId") UUID quizId);

    /**
     * Bài làm trên một quiz, kèm số câu <b>cần chấm tay</b>.
     * <p>
     * Đây là cửa vào của việc chấm tay — món nợ từ features/06: API ghi đè điểm đã có từ lát cắt đó
     * nhưng Creator không có cách nào tìm ra bài nào cần chấm.
     */
    @Query("""
            select a.id as attemptId,
                   a.user.displayName as learnerName,
                   a.totalScore as score,
                   a.maxScore as maxScore,
                   a.submittedAt as submittedAt,
                   sum(case when ans.gradedBy = com.datn.quizai.attempt.domain.GradedBy.PENDING_AI
                            then 1 else 0 end) as pendingAiCount,
                   sum(case when ans.gradedBy = com.datn.quizai.attempt.domain.GradedBy.AI_FAILED
                            then 1 else 0 end) as failedAiCount
            from QuizAttempt a
              left join a.answers ans
            where a.quiz.id = :quizId
              and a.status <> com.datn.quizai.attempt.domain.AttemptStatus.IN_PROGRESS
            group by a.id, a.user.displayName, a.totalScore, a.maxScore, a.submittedAt
            order by a.submittedAt desc
            """)
    List<QuizAttemptRow> findQuizAttempts(@Param("quizId") UUID quizId);

    // ================================================================ Projection

    interface LearnerOverviewRow {
        long getTotalAttempts();

        long getDistinctQuizzes();

        long getSumScore();

        long getSumMaxScore();
    }

    interface AttemptScoreRow {
        java.time.OffsetDateTime getSubmittedAt();

        String getQuizTitle();

        int getScore();

        int getMaxScore();
    }

    interface QuizOverviewRow {
        long getTotalAttempts();

        long getDistinctLearners();

        long getSumScore();

        long getSumMaxScore();

        /** Số lượt nộp kịp giờ; phần còn lại là lượt bị hết giờ. */
        long getSubmittedCount();
    }

    interface ScoreBucketRow {
        int getBucket();

        long getAttemptCount();
    }

    interface HardQuestionRow {
        UUID getQuestionId();

        String getContent();

        String getTopic();

        long getAnsweredCount();

        long getWrongCount();
    }

    interface QuizAttemptRow {
        UUID getAttemptId();

        String getLearnerName();

        int getScore();

        int getMaxScore();

        java.time.OffsetDateTime getSubmittedAt();

        long getPendingAiCount();

        long getFailedAiCount();
    }
}
