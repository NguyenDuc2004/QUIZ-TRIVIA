package com.datn.quizai.gamification.repository;

import com.datn.quizai.gamification.domain.UserDailyProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserDailyProgressRepository extends JpaRepository<UserDailyProgress, UUID> {

    Optional<UserDailyProgress> findByUserIdAndChallengeId(UUID userId, UUID challengeId);
}
