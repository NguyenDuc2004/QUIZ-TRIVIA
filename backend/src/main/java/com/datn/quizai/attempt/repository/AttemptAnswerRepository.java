package com.datn.quizai.attempt.repository;

import com.datn.quizai.attempt.domain.AttemptAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Truy cập thẳng từng câu trả lời.
 * <p>
 * Tách khỏi {@code QuizAttemptRepository} vì luồng chấm AI (features/06) làm việc trên <b>một câu
 * một lúc</b>, mỗi câu một transaction ngắn. Nạp cả bài chỉ để sửa một câu là thừa, và giữ cả đồ
 * thị đối tượng suốt thời gian gọi mô hình thì càng tệ.
 */
public interface AttemptAnswerRepository extends JpaRepository<AttemptAnswer, UUID> {

    /**
     * Những câu đã nộp đang chờ AI chấm, kèm sẵn câu hỏi và lựa chọn.
     * <p>
     * {@code join fetch} là bắt buộc: luồng chấm chạy ngoài transaction, chạm vào quan hệ lazy sau
     * đó sẽ ném {@code LazyInitializationException}. Phải lấy đủ ngay từ truy vấn này.
     */
    @Query("""
            select distinct a from AttemptAnswer a
            join fetch a.question q
            left join fetch q.options
            where a.attempt.id = :attemptId and a.gradedBy = com.datn.quizai.attempt.domain.GradedBy.PENDING_AI
            order by a.orderIndex
            """)
    List<AttemptAnswer> findPendingAiByAttempt(@Param("attemptId") UUID attemptId);

    /** Nạp một câu kèm câu hỏi — dùng khi chấm lại hoặc giải thích một câu lẻ. */
    @Query("""
            select a from AttemptAnswer a
            join fetch a.question q
            left join fetch q.options
            where a.id = :id
            """)
    java.util.Optional<AttemptAnswer> findByIdWithQuestion(@Param("id") UUID id);
}
