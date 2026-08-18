package com.datn.quizai.notification.repository;

import com.datn.quizai.notification.domain.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Chèn thông báo, <b>bỏ qua trong im lặng</b> nếu đã có cái cùng khoá chống trùng.
     *
     * <h3>Vì sao là SQL thuần với {@code ON CONFLICT} chứ không phải {@code save()} rồi bắt ngoại lệ</h3>
     * Cách hiển nhiên hơn — {@code save()} trong {@code try}, bắt {@code DataIntegrityViolationException} —
     * <b>không chạy được</b>, và đây là lần thứ hai trong dự án gặp đúng cặp bẫy này:
     * <ol>
     *   <li>{@code save()} của JPA <b>chưa gửi câu lệnh xuống cơ sở dữ liệu ngay</b>. Vi phạm ràng buộc nổ
     *       lúc flush/commit, tức là <i>sau khi</i> thân phương thức đã ra khỏi khối {@code catch}. Ngoại lệ
     *       thoát lên người gọi và không ai bắt được nó ở chỗ đáng bắt.</li>
     *   <li>Chữa bằng {@code saveAndFlush} để ngoại lệ nổ trong {@code try} thì rơi vào bẫy thứ hai: Spring
     *       đã đánh dấu transaction là rollback-only, nên bắt rồi trả về bình thường vẫn vỡ ở lần commit
     *       với {@code UnexpectedRollbackException} — lỗi nổ ra ở một chỗ chẳng liên quan gì.</li>
     * </ol>
     * {@code ON CONFLICT DO NOTHING} thoát khỏi cả hai: <b>không có ngoại lệ nào</b> để bắt, transaction
     * không bị đánh dấu gì. Trùng khoá là đường chạy bình thường của một job hằng ngày, nên nó không nên đi
     * qua cơ chế ngoại lệ ngay từ đầu.
     * <p>
     * {@code cast(:data as jsonb)} là bắt buộc — truyền chuỗi thẳng vào cột {@code jsonb} thì PostgreSQL từ
     * chối. Đây là chỗ tương ứng với {@code @JdbcTypeCode(SqlTypes.JSON)} ở đường JPA.
     *
     * @return 1 nếu vừa chèn, 0 nếu đã có cái cùng khoá
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            insert into notifications (id, user_id, type, title, body, data, dedupe_key, is_read, created_at)
            values (:id, :userId, :type, :title, :body, cast(:data as jsonb), :dedupeKey, false, now())
            on conflict (user_id, dedupe_key) do nothing
            """, nativeQuery = true)
    int chenNeuChuaCo(@Param("id") UUID id,
                      @Param("userId") UUID userId,
                      @Param("type") String type,
                      @Param("title") String title,
                      @Param("body") String body,
                      @Param("data") String data,
                      @Param("dedupeKey") String dedupeKey);

    long countByUserIdAndReadFalse(UUID userId);

    /**
     * Đánh dấu đã đọc toàn bộ trong <b>một</b> câu lệnh.
     * <p>
     * Không nạp về rồi lặp {@code setRead(true)}: một người dùng lâu năm có hàng nghìn thông báo, và nạp cả
     * nghìn entity vào bộ nhớ để đổi một cột boolean là tốn vô ích. Lọc {@code read = false} ngay trong câu
     * lệnh để không ghi lại những dòng đã đọc từ lâu.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Notification n set n.read = true where n.userId = :userId and n.read = false")
    int markAllRead(@Param("userId") UUID userId);
}
