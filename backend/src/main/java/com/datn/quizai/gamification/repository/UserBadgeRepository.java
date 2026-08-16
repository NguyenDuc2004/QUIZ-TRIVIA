package com.datn.quizai.gamification.repository;

import com.datn.quizai.gamification.domain.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserBadgeRepository extends JpaRepository<UserBadge, UUID> {

    /** Nạp kèm định nghĩa huy hiệu để map sang DTO không sinh N+1. */
    @Query("select ub from UserBadge ub join fetch ub.badge where ub.userId = :userId order by ub.earnedAt desc")
    List<UserBadge> findByUserIdWithBadge(@Param("userId") UUID userId);

    boolean existsByUserIdAndBadgeId(UUID userId, UUID badgeId);
}
