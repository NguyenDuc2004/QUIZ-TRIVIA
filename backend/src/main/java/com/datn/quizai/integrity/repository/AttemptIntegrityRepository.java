package com.datn.quizai.integrity.repository;

import com.datn.quizai.integrity.domain.AttemptIntegrity;
import com.datn.quizai.integrity.domain.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AttemptIntegrityRepository extends JpaRepository<AttemptIntegrity, UUID> {

    Optional<AttemptIntegrity> findByAttemptId(UUID attemptId);

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
