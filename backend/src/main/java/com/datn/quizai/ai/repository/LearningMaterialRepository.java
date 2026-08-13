package com.datn.quizai.ai.repository;

import com.datn.quizai.ai.domain.LearningMaterial;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface LearningMaterialRepository extends JpaRepository<LearningMaterial, UUID> {

    @Query("""
            select m from LearningMaterial m
            where m.owner.id = :ownerId
            order by m.createdAt desc
            """)
    Page<LearningMaterial> findMine(@Param("ownerId") UUID ownerId, Pageable pageable);

    /**
     * Học liệu mà người gọi <b>được phép hỏi trợ lý</b>: tài liệu của chính họ, cộng tài liệu người
     * khác đã chủ động bật {@code shared}.
     * <p>
     * Đúng cùng một phạm vi mà {@code MaterialChunkRepository#searchSimilarIncludingShared} truy hồi
     * — nếu hai chỗ lệch nhau thì giao diện liệt kê một danh sách khác với thứ trợ lý thật sự đọc
     * được, và người dùng sẽ thấy tài liệu trong danh sách mà hỏi mãi không ra.
     * <p>
     * Chỉ lấy tài liệu {@code READY}: tài liệu đang xử lý chưa có vector nên hỏi cũng không truy hồi
     * được gì, liệt kê ra chỉ khiến người học tưởng hỏi được rồi nhận về câu "không biết".
     */
    @Query("""
            select m from LearningMaterial m
            where (m.owner.id = :userId or m.shared = true)
              and m.status = com.datn.quizai.ai.domain.MaterialStatus.READY
            order by m.createdAt desc
            """)
    List<LearningMaterial> findAskable(@Param("userId") UUID userId, Limit limit);
}
