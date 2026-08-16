package com.datn.quizai.flashcard.domain;

import com.datn.quizai.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Trạng thái lặp lại ngắt quãng của <b>một người dùng</b> trên <b>một thẻ</b> — bảng
 * `flashcard_reviews` (features/11, FR-40).
 * <p>
 * Đây là bảng riêng chứ không phải mấy cột trên {@link Flashcard}, vì một thẻ có thể được nhiều người ôn
 * và mỗi người có lịch riêng. Nhét {@code dueDate}/{@code easeFactor} vào thẻ thì hai người ôn cùng một
 * bộ sẽ ghi đè lịch của nhau.
 * <p>
 * Entity chỉ giữ dữ liệu; phép tính SM-2 nằm ở tầng service (`docs/conventions.md` — không đặt logic
 * nghiệp vụ ở entity).
 */
@Entity
@Table(name = "flashcard_reviews")
@Getter
@Setter
@NoArgsConstructor
public class FlashcardReview {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "flashcard_id", nullable = false)
    private Flashcard flashcard;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Hệ số dễ của SM-2. Càng thấp thì khoảng ôn giãn càng chậm; sàn 1.30 là của thuật toán — thấp hơn
     * nữa thì thẻ quay lại quá dày và người học không bao giờ thoát khỏi nó.
     * <p>
     * Dùng {@link BigDecimal} khớp {@code NUMERIC(4,2)} thay vì {@code double}: hệ số này được cộng trừ
     * nhiều lần liên tiếp, và sai số nhị phân tích lại sẽ trôi dần khỏi sàn 1.30 lẽ ra phải chặn được.
     */
    @Column(name = "ease_factor", nullable = false, precision = 4, scale = 2)
    private BigDecimal easeFactor = new BigDecimal("2.50");

    /** Khoảng cách tới lần ôn kế tiếp, tính bằng ngày. 0 = thẻ chưa ôn lần nào. */
    @Column(name = "interval_days", nullable = false)
    private int intervalDays = 0;

    /** Số lần trả lời tốt <b>liên tiếp</b>. Về 0 mỗi lần trả lời kém — đây là biến của thuật toán. */
    @Column(nullable = false)
    private int repetitions = 0;

    /** Ngày đến hạn ôn. Thẻ mới đến hạn ngay hôm tạo. */
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate = LocalDate.now();

    @Column(name = "last_reviewed_at")
    private OffsetDateTime lastReviewedAt;

    /**
     * Tổng số lần đã ôn, <b>khác</b> {@link #repetitions}: cột kia bị reset về 0 mỗi lần trả lời sai,
     * còn cột này chỉ tăng. Cần cả hai — một để chạy thuật toán, một để thống kê thật số lần đã ôn.
     */
    @Column(name = "total_reviews", nullable = false)
    private int totalReviews = 0;

    /** Số lần "quên" — trả lời kém sau khi đã từng thuộc. Dấu hiệu thẻ này khó với người đó. */
    @Column(nullable = false)
    private int lapses = 0;
}
