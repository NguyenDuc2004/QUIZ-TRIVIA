package com.datn.quizai.flashcard.service;

import com.datn.quizai.common.exception.BusinessException;
import com.datn.quizai.flashcard.domain.FlashcardReview;
import com.datn.quizai.flashcard.domain.ReviewQuality;
import com.datn.quizai.flashcard.dto.FlashcardResponse;
import com.datn.quizai.flashcard.dto.ReviewResult;
import com.datn.quizai.flashcard.dto.ReviewStats;
import com.datn.quizai.flashcard.repository.FlashcardReviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Phiên ôn tập và lịch lặp lại ngắt quãng (features/11, FR-40 và FR-41).
 * <p>
 * Phép tính SM-2 nằm ở {@link Sm2Scheduler}; lớp này chỉ đọc trạng thái, gọi thuật toán, rồi ghi lại. Tách
 * như vậy để kiểm được thuật toán mà không cần cơ sở dữ liệu, và kiểm được phần lưu trữ mà không phải suy
 * ra khoảng ôn bằng đầu.
 */
@Service
public class ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewService.class);

    /** Số ngày dự báo khối lượng ôn. Một tuần: đủ để thấy ngày dồn thẻ, chưa tới mức vô nghĩa. */
    private static final int SO_NGAY_DU_BAO = 7;

    private final FlashcardReviewRepository reviewRepository;
    private final ApplicationEventPublisher events;

    public ReviewService(FlashcardReviewRepository reviewRepository,
                         ApplicationEventPublisher events) {
        this.reviewRepository = reviewRepository;
        this.events = events;
    }

    /**
     * Thẻ đến hạn ôn, quá hạn lâu nhất lên trước (FR-41).
     *
     * @param deckId giới hạn trong một bộ; {@code null} = mọi bộ của người này
     */
    @Transactional(readOnly = true)
    public List<FlashcardResponse> due(UUID userId, UUID deckId) {
        return reviewRepository.findDue(userId, deckId, LocalDate.now()).stream()
                .map(r -> FlashcardResponse.from(r.getFlashcard(), r))
                .toList();
    }

    /**
     * Ghi kết quả ôn một thẻ và tính lịch kế tiếp (FR-40).
     * <p>
     * Không kiểm "thẻ này có đang đến hạn không" trước khi cho ôn: ôn sớm một thẻ chưa tới hạn là việc hợp
     * lệ và có ích, chặn lại chỉ làm người học thấy vô lý. Thuật toán tự xử lý được vì nó tính từ trạng
     * thái hiện tại, không tính từ việc hôm nay là ngày nào.
     */
    @Transactional
    public ReviewResult review(UUID cardId, UUID userId, ReviewQuality quality) {
        FlashcardReview review = reviewRepository.findByFlashcardIdAndUserId(cardId, userId)
                // Không có trạng thái ôn nghĩa là thẻ không thuộc người này, hoặc không tồn tại. Cả hai
                // trả 404 như nhau để không tiết lộ thẻ của người khác có thật hay không.
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy thẻ để ôn"));

        var lich = Sm2Scheduler.tinh(review.getEaseFactor(), review.getIntervalDays(),
                review.getRepetitions(), quality);

        review.setEaseFactor(lich.easeFactor());
        review.setIntervalDays(lich.intervalDays());
        review.setRepetitions(lich.repetitions());
        review.setDueDate(LocalDate.now().plusDays(lich.intervalDays()));
        review.setLastReviewedAt(OffsetDateTime.now());
        review.setTotalReviews(review.getTotalReviews() + 1);
        if (lich.laLanQuen()) {
            review.setLapses(review.getLapses() + 1);
        }

        // Đếm SAU khi cập nhật: thẻ vừa ôn xong đã có due_date ở tương lai nên không còn nằm trong số
        // đến hạn. Đếm trước thì thanh tiến độ của phiên ôn luôn lệch một thẻ.
        long conLai = reviewRepository.countByUserIdAndDueDateLessThanEqual(userId, LocalDate.now());

        log.debug("Người dùng {} ôn thẻ {} mức {} → cách {} ngày", userId, cardId, quality,
                lich.intervalDays());

        // Phát sự kiện để gamification cộng XP. KHÔNG gọi thẳng GamificationService: một lỗi ở phần trò
        // chơi hoá không được làm vỡ luồng ôn thẻ, và ôn thẻ không cần biết gamification tồn tại.
        events.publishEvent(new FlashcardReviewedEvent(userId, cardId, LocalDate.now()));

        return new ReviewResult(review.getDueDate(), lich.intervalDays(), lich.repetitions(), conLai);
    }

    /** Thống kê ôn tập (FR-42). */
    @Transactional(readOnly = true)
    public ReviewStats stats(UUID userId) {
        LocalDate homNay = LocalDate.now();
        LocalDate den = homNay.plusDays(SO_NGAY_DU_BAO - 1L);

        Map<LocalDate, Long> theoNgay = reviewRepository.duBao(userId, homNay, den).stream()
                .collect(Collectors.toMap(FlashcardReviewRepository.DuBaoRow::getNgay,
                        FlashcardReviewRepository.DuBaoRow::getSoThe));

        // Bù ngày không có thẻ thành giá trị 0. Thiếu bước này thì biểu đồ dự báo nhảy qua ngày trống và
        // trông như khối lượng ôn liên tục — đúng cùng một lỗi đã xử lý ở trang tổng quan quản trị.
        List<ReviewStats.DiemDuBao> duBao = new ArrayList<>(SO_NGAY_DU_BAO);
        for (int i = 0; i < SO_NGAY_DU_BAO; i++) {
            LocalDate ngay = homNay.plusDays(i);
            duBao.add(new ReviewStats.DiemDuBao(ngay, theoNgay.getOrDefault(ngay, 0L)));
        }

        return new ReviewStats(
                reviewRepository.countAll(userId),
                reviewRepository.countMastered(userId),
                reviewRepository.countByUserIdAndDueDateLessThanEqual(userId, homNay),
                duBao);
    }
}
