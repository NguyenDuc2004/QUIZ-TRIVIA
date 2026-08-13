package com.datn.quizai.chat.repository;

import com.datn.quizai.chat.domain.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {

    /**
     * Phiên của một người, mới hoạt động nhất trước.
     * <p>
     * Xếp theo {@code updatedAt} chứ không {@code createdAt}: phiên mở tuần trước mà vẫn đang dùng
     * phải nằm trên phiên mở hôm qua rồi bỏ đó.
     */
    List<ChatSession> findByUserIdOrderByUpdatedAtDesc(UUID userId);

    /** Nạp phiên và kiểm chủ sở hữu trong <b>một</b> truy vấn — không có đường quên kiểm. */
    @Query("select s from ChatSession s where s.id = :id and s.user.id = :userId")
    Optional<ChatSession> findOwned(@Param("id") UUID id, @Param("userId") UUID userId);

    /**
     * Đẩy {@code updated_at} lên hiện tại.
     * <p>
     * Viết thẳng bằng UPDATE thay vì nạp entity rồi sửa: đây là thao tác chạy sau khi câu trả lời đã
     * stream xong, ngoài transaction gốc, và chỉ cần đúng một cột. `@UpdateTimestamp` không tự chạy
     * khi không có trường nào khác thay đổi.
     */
    @Modifying
    @Query("update ChatSession s set s.updatedAt = CURRENT_TIMESTAMP where s.id = :id")
    void touch(@Param("id") UUID id);
}
