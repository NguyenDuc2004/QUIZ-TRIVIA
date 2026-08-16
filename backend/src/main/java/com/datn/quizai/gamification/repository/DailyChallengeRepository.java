package com.datn.quizai.gamification.repository;

import com.datn.quizai.gamification.domain.DailyChallenge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface DailyChallengeRepository extends JpaRepository<DailyChallenge, UUID> {

    Optional<DailyChallenge> findByChallengeDate(LocalDate date);
}
