package com.datn.quizai.integrity.repository;

import com.datn.quizai.integrity.domain.AttemptIntegrity;
import com.datn.quizai.integrity.domain.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttemptIntegrityRepository extends JpaRepository<AttemptIntegrity, UUID> {

    Optional<AttemptIntegrity> findByAttemptId(UUID attemptId);

    /**
     * Nạp một lượt cho nhiều lượt thi — dùng cho danh sách bài làm của chủ quiz.
     * <p>
     * Có phương thức này để trang thống kê <b>không</b> gọi {@link #findByAttemptId} trong vòng lặp: một quiz
     * có 200 bài nộp thì đó là 200 truy vấn cho một cột hiển thị.
     */
    List<AttemptIntegrity> findByAttemptIdIn(Collection<UUID> attemptIds);

    /**
     * Bài bị gắn cờ, điểm rủi ro cao trước.
     * <p>
     * Lọc theo ngưỡng ở truy vấn chứ không lọc trong Java: danh sách này có thể dài, và kéo cả bảng về để bỏ
     * đi phần lớn là tốn vô ích.
     */
    @Query("""
            select i from AttemptIntegrity i
            where i.riskScore >= :nguong
              and (:status is null or i.reviewStatus = :status)
            order by i.riskScore desc, i.createdAt desc
            """)
    Page<AttemptIntegrity> findFlagged(@Param("nguong") int nguong,
                                       @Param("status") ReviewStatus status,
                                       Pageable pageable);
}
