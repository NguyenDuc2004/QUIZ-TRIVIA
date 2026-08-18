package com.datn.quizai.season.repository;

import com.datn.quizai.season.domain.SeasonRanking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SeasonRankingRepository extends JpaRepository<SeasonRanking, UUID> {

    boolean existsBySeasonId(UUID seasonId);

    /** Top của một mùa đã chốt. Nạp kèm huy hiệu để map sang DTO không sinh N+1. */
    @Query("""
            select r from SeasonRanking r
              left join fetch r.rewardBadge
            where r.season.id = :seasonId
            order by r.finalRank
            """)
    List<SeasonRanking> findTopBySeason(@Param("seasonId") UUID seasonId);

    /** Thành tích các mùa trước của một người. */
    @Query("""
            select r from SeasonRanking r
              join fetch r.season s
              left join fetch r.rewardBadge
            where r.userId = :userId
            order by s.endAt desc
            """)
    List<SeasonRanking> findHistoryOfUser(@Param("userId") UUID userId);
}
