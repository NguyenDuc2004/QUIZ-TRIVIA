package com.datn.quizai.gamification.service;

import com.datn.quizai.attempt.service.AttemptSubmittedEvent;
import com.datn.quizai.flashcard.service.FlashcardReviewedEvent;
import com.datn.quizai.gamification.domain.XpSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

/**
 * Nối gamification vào các hành động học tập (features/13, FR-49).
 *
 * <h3>Vì sao là listener, không phải lời gọi trực tiếp</h3>
 * Service làm bài và service ôn thẻ <b>không biết gamification tồn tại</b>. Chúng chỉ phát sự kiện. Nhờ vậy
 * một lỗi ở phần trò chơi hoá không làm vỡ luồng nộp bài — và quan trọng hơn, có thể bỏ hẳn tính năng này mà
 * không phải sờ vào hai service kia.
 *
 * <h3>Vì sao {@code @TransactionalEventListener}</h3>
 * Chạy <b>sau khi</b> transaction nghiệp vụ commit. Chạy trong cùng transaction thì hai hậu quả xấu: cộng XP
 * cho một bài làm rồi transaction đó rollback, và một lỗi ràng buộc ở gamification kéo cả việc nộp bài
 * rollback theo.
 */
@Component
public class GamificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(GamificationEventListener.class);

    /** XP cho mỗi bài nộp, bất kể điểm — thưởng cho việc làm bài, không chỉ cho việc làm đúng. */
    private static final int XP_NOP_BAI = 20;

    /** Thưởng thêm khi làm đúng 100%. */
    private static final int XP_THUONG_HOAN_HAO = 15;

    /**
     * XP cho mỗi thẻ ôn. Nhỏ hơn nhiều so với làm bài, và chỉ tính một lần mỗi thẻ mỗi ngày (khoá
     * {@code cardId:ngày}) — nếu không thì bấm một thẻ liên tục là cách kiếm XP nhanh nhất, và con số mất
     * hết ý nghĩa.
     */
    private static final int XP_ON_THE = 3;

    private final GamificationService gamificationService;
    private final DailyChallengeService dailyChallengeService;
    private final JdbcTemplate jdbc;

    public GamificationEventListener(GamificationService gamificationService,
                                    DailyChallengeService dailyChallengeService,
                                    JdbcTemplate jdbc) {
        this.gamificationService = gamificationService;
        this.dailyChallengeService = dailyChallengeService;
        this.jdbc = jdbc;
    }

    // REQUIRES_NEW là bắt buộc, không phải lựa chọn: `@TransactionalEventListener` chạy SAU khi transaction
    // nghiệp vụ commit, nên không còn transaction nào để tham gia. Spring từ chối khởi động nếu để mặc định
    // REQUIRED. Về nghĩa cũng đúng — phần cộng XP cần transaction riêng của nó.
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAttemptSubmitted(AttemptSubmittedEvent event) {
        try {
            boolean hoanHao = laBaiHoanHao(event.attemptId());
            int xp = XP_NOP_BAI + (hoanHao ? XP_THUONG_HOAN_HAO : 0);

            boolean vuaCong = gamificationService.award(event.userId(), XpSource.ATTEMPT_SUBMITTED,
                    event.attemptId().toString(), xp);

            // Chỉ ghi tiến độ thử thách khi XP thật sự vừa được cộng: nếu sự kiện này chạy lại (retry) thì
            // `award` trả false, và tiến độ không được cộng thêm lần nữa.
            if (vuaCong) {
                dailyChallengeService.ghiNhanHanhDong(event.userId(), "ATTEMPT");
                if (hoanHao) {
                    dailyChallengeService.ghiNhanHanhDong(event.userId(), "PERFECT_ATTEMPT");
                }
            }
        } catch (Exception e) {
            // Nuốt lỗi có chủ đích: bài đã nộp xong và đã commit. Làm nổi ngoại lệ ở đây chỉ ghi một vết
            // lỗi vào log của luồng nền, không sửa được gì, và tệ nhất là làm người dùng tưởng bài không nộp
            // được. Ghi log để còn truy được.
            log.warn("Không cộng được XP cho bài làm {}: {}", event.attemptId(), e.getMessage());
        }
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onFlashcardReviewed(FlashcardReviewedEvent event) {
        try {
            // Khoá gồm cả ngày — xem XpSource.FLASHCARD_REVIEW
            String khoa = event.flashcardId() + ":" + event.ngay();
            if (gamificationService.award(event.userId(), XpSource.FLASHCARD_REVIEW, khoa, XP_ON_THE)) {
                dailyChallengeService.ghiNhanHanhDong(event.userId(), "FLASHCARD_REVIEW");
            }
        } catch (Exception e) {
            log.warn("Không cộng được XP cho lượt ôn thẻ {}: {}", event.flashcardId(), e.getMessage());
        }
    }

    /**
     * Bài làm có đúng 100% hay không.
     * <p>
     * {@code max_score > 0} để loại bài không có câu nào: bài rỗng thì {@code 0 = 0} và sẽ được tính là hoàn
     * hảo, tức thưởng XP cho một bài không làm gì.
     */
    private boolean laBaiHoanHao(UUID attemptId) {
        Boolean hoanHao = jdbc.queryForObject("""
                select max_score > 0 and total_score = max_score
                from quiz_attempts where id = ?
                """, Boolean.class, attemptId);
        return Boolean.TRUE.equals(hoanHao);
    }
}
