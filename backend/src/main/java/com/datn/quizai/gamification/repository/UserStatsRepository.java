package com.datn.quizai.gamification.repository;

import com.datn.quizai.gamification.domain.UserStats;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserStatsRepository extends JpaRepository<UserStats, UUID> {
}
