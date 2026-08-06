package com.datn.quizai.ai.repository;

import com.datn.quizai.ai.domain.AiJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AiJobRepository extends JpaRepository<AiJob, UUID> {
}
