package com.datn.quizai.gamification.repository;

import com.datn.quizai.gamification.domain.Badge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BadgeRepository extends JpaRepository<Badge, UUID> {

    List<Badge> findAllByOrderBySortOrderAsc();
}
