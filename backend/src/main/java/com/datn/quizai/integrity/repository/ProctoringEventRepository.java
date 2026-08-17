package com.datn.quizai.integrity.repository;

import com.datn.quizai.integrity.domain.ProctoringEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProctoringEventRepository extends JpaRepository<ProctoringEvent, UUID> {

    List<ProctoringEvent> findByAttemptIdOrderByOccurredAt(UUID attemptId);

    long countByAttemptId(UUID attemptId);
}
