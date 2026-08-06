package com.datn.quizai.ai.repository;

import com.datn.quizai.ai.domain.LearningMaterial;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface LearningMaterialRepository extends JpaRepository<LearningMaterial, UUID> {

    @Query("""
            select m from LearningMaterial m
            where m.owner.id = :ownerId
            order by m.createdAt desc
            """)
    Page<LearningMaterial> findMine(@Param("ownerId") UUID ownerId, Pageable pageable);
}
