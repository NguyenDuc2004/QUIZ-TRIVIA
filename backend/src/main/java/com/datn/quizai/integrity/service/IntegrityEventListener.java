package com.datn.quizai.integrity.service;

import com.datn.quizai.attempt.domain.AttemptMode;
import com.datn.quizai.attempt.repository.QuizAttemptRepository;
import com.datn.quizai.attempt.service.AttemptSubmittedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Tính tính toàn vẹn ngay sau khi nộp bài thi (features/12, FR-45).
 * <p>
 * Là listener chứ không phải lời gọi trong {@code AttemptService}, cùng lý do như gamification: luồng nộp bài
 * không biết tính năng này tồn tại, nên một lỗi ở đây không làm người dùng mất bài. Và {@code REQUIRES_NEW}
 * vì {@code @TransactionalEventListener} chạy sau khi transaction nghiệp vụ đã commit.
 * <p>
 * <b>Chỉ chạy cho lượt {@code EXAM}.</b> Luyện tập không thu tín hiệu nên cũng không có gì để tính, và tạo một
 * bản tổng hợp rỗng cho mỗi lượt luyện tập chỉ làm bảng phình và làm loãng danh sách rà soát.
 */
@Component
public class IntegrityEventListener {

    private static final Logger log = LoggerFactory.getLogger(IntegrityEventListener.class);

    private final IntegrityService integrityService;
    private final QuizAttemptRepository attemptRepository;

    public IntegrityEventListener(IntegrityService integrityService,
                                  QuizAttemptRepository attemptRepository) {
        this.integrityService = integrityService;
        this.attemptRepository = attemptRepository;
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAttemptSubmitted(AttemptSubmittedEvent event) {
        try {
            var attempt = attemptRepository.findById(event.attemptId()).orElse(null);
            if (attempt == null || attempt.getMode() != AttemptMode.EXAM) {
                return;
            }
            var integrity = integrityService.tinhLai(event.attemptId(), event.userId());

            if (integrity.getRiskScore() >= RiskScorer.NGUONG_GAN_CO) {
                // Ghi log ở mức info cho bài bị gắn cờ: đây là thứ người vận hành cần thấy, và cũng là dấu
                // vết để đối chiếu về sau nếu có tranh chấp. KHÔNG ghi tên hay email — chỉ id.
                log.info("Lượt thi {} bị gắn cờ: điểm rủi ro {}", event.attemptId(), integrity.getRiskScore());
            }
        } catch (Exception e) {
            // Nuốt có chủ đích: bài đã nộp và đã commit. Làm nổi ngoại lệ ở đây không sửa được gì, mà tệ nhất
            // là làm người dùng tưởng bài không nộp được.
            log.warn("Không tính được tính toàn vẹn cho lượt {}: {}", event.attemptId(), e.getMessage());
        }
    }
}
