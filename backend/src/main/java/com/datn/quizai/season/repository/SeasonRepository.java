package com.datn.quizai.season.repository;

import com.datn.quizai.season.domain.Season;
import com.datn.quizai.season.domain.SeasonStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SeasonRepository extends JpaRepository<Season, UUID> {

    Optional<Season> findByStatus(SeasonStatus status);

    /** Mùa đã kết thúc, mới nhất trước — dùng cho lịch sử. */
    List<Season> findByStatusOrderByEndAtDesc(SeasonStatus status);

    /** Mùa đang chạy mà đã quá hạn — điều kiện để job chốt mùa ra tay. */
    Optional<Season> findFirstByStatusAndEndAtBefore(SeasonStatus status, OffsetDateTime moc);
}
