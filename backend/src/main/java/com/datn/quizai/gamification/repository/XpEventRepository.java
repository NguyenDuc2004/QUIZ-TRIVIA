package com.datn.quizai.gamification.repository;

import com.datn.quizai.gamification.domain.XpEvent;
import com.datn.quizai.gamification.domain.XpSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface XpEventRepository extends JpaRepository<XpEvent, UUID> {

    /**
     * Đã cộng XP cho hành động này chưa.
     * <p>
     * Kiểm trước để không ném ngoại lệ ràng buộc trong luồng bình thường. Ràng buộc UNIQUE ở cơ sở dữ liệu
     * vẫn là chốt cuối — kiểm ở đây thua cuộc khi hai luồng chạy song song, nên cần cả hai.
     */
    boolean existsByUserIdAndSourceTypeAndSourceKey(UUID userId, XpSource sourceType, String sourceKey);
}
