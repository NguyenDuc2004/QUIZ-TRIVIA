package com.datn.quizai.flashcard.repository;

import com.datn.quizai.flashcard.domain.FlashcardReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FlashcardReviewRepository extends JpaRepository<FlashcardReview, UUID> {

    Optional<FlashcardReview> findByFlashcardIdAndUserId(UUID flashcardId, UUID userId);

    List<FlashcardReview> findByUserIdAndFlashcardDeckId(UUID userId, UUID deckId);

    /**
     * Thẻ đến hạn ôn của một người (FR-41).
     * <p>
     * {@code dueDate <= hôm nay} chứ không phải {@code = hôm nay}: thẻ quá hạn từ những ngày người học
     * không mở ứng dụng vẫn phải hiện ra. Lọc bằng {@code =} thì nghỉ một ngày là mất luôn số thẻ của
     * ngày đó, và đó đúng là lúc người ta cần ôn nhất.
     * <p>
     * Sắp theo {@code dueDate} tăng: thẻ quá hạn lâu nhất lên trước.
     */
    @Query("""
            select r from FlashcardReview r
              join fetch r.flashcard c
              join fetch c.deck d
            where r.user.id = :userId
              and r.dueDate <= :homNay
              and (:deckId is null or d.id = :deckId)
            order by r.dueDate, c.createdAt
            """)
    List<FlashcardReview> findDue(@Param("userId") UUID userId,
                                  @Param("deckId") UUID deckId,
                                  @Param("homNay") LocalDate homNay);

    long countByUserIdAndDueDateLessThanEqual(UUID userId, LocalDate ngay);

    /**
     * Số thẻ đến hạn của một người, <b>gộp theo bộ thẻ</b>.
     * <p>
     * Một truy vấn cho cả danh sách bộ thẻ. Cách hiển nhiên hơn — đếm riêng cho từng bộ — là N+1 lượt đi
     * vòng tới cơ sở dữ liệu, và nếu đếm bằng cách nạp entity rồi lấy {@code size()} thì còn kéo về cả
     * nội dung thẻ chỉ để đếm.
     */
    @Query("""
            select d.id as deckId, count(r) as soThe
            from FlashcardReview r
              join r.flashcard c
              join c.deck d
            where r.user.id = :userId
              and r.dueDate <= :homNay
            group by d.id
            """)
    List<DenHanTheoBoRow> demDenHanTheoBo(@Param("userId") UUID userId,
                                          @Param("homNay") LocalDate homNay);

    interface DenHanTheoBoRow {
        UUID getDeckId();

        long getSoThe();
    }

    /** Số thẻ đến hạn trong một bộ cụ thể. */
    @Query("""
            select count(r) from FlashcardReview r
            where r.user.id = :userId
              and r.flashcard.deck.id = :deckId
              and r.dueDate <= :homNay
            """)
    long demDenHanTrongBo(@Param("userId") UUID userId,
                          @Param("deckId") UUID deckId,
                          @Param("homNay") LocalDate homNay);

    /**
     * Số thẻ "đã thuộc" — khoảng ôn từ 21 ngày trở lên.
     * <p>
     * 21 ngày là ngưỡng quy ước của SM-2 để coi một thẻ đã chuyển sang ghi nhớ dài hạn. Đây là một lựa
     * chọn hiển thị, không phải kết quả đo, nên giao diện phải nói rõ ngưỡng thay vì chỉ ghi "đã thuộc".
     */
    @Query("select count(r) from FlashcardReview r where r.user.id = :userId and r.intervalDays >= 21")
    long countMastered(@Param("userId") UUID userId);

    @Query("select count(r) from FlashcardReview r where r.user.id = :userId")
    long countAll(@Param("userId") UUID userId);

    /**
     * Khối lượng ôn dự báo cho {@code soNgay} ngày tới, gộp theo ngày.
     * <p>
     * Chỉ trả những ngày <b>có thẻ</b>; ngày trống do tầng trên bù vào. Ở đây không dùng
     * {@code generate_series} như `OverviewRepository` vì đây là JPQL — bù ngày trống trong Java rẻ hơn
     * là đổi sang SQL thuần chỉ để lấy mấy dòng số.
     */
    @Query("""
            select r.dueDate as ngay, count(r) as soThe
            from FlashcardReview r
            where r.user.id = :userId
              and r.dueDate between :tu and :den
            group by r.dueDate
            order by r.dueDate
            """)
    List<DuBaoRow> duBao(@Param("userId") UUID userId,
                         @Param("tu") LocalDate tu,
                         @Param("den") LocalDate den);

    interface DuBaoRow {
        LocalDate getNgay();

        long getSoThe();
    }

    /**
     * <b>Ai</b> có thẻ đến hạn, gộp theo người dùng — nguồn của job nhắc ôn tập (features/16, FR-66).
     * <p>
     * Đi từ phía "cả hệ thống" xuống, ngược với mọi truy vấn khác trong repository này vốn đi từ một người.
     * Cách hiển nhiên hơn là lấy danh sách người dùng rồi đếm cho từng người, nhưng đó là một truy vấn cho
     * <i>mỗi</i> tài khoản trong hệ thống mỗi ngày — kể cả người chưa từng tạo một thẻ nào. Một câu
     * {@code group by} chỉ trả về đúng những người thật sự có thẻ đến hạn.
     */
    @Query("""
            select r.user.id as userId, count(r) as soThe
            from FlashcardReview r
            where r.dueDate <= :homNay
            group by r.user.id
            """)
    List<NguoiDenHanRow> demDenHanTheoNguoi(@Param("homNay") LocalDate homNay);

    interface NguoiDenHanRow {
        UUID getUserId();

        long getSoThe();
    }
}
