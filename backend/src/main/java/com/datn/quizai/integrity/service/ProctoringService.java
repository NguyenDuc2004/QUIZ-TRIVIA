package com.datn.quizai.integrity.service;

import com.datn.quizai.attempt.domain.AttemptMode;
import com.datn.quizai.attempt.domain.QuizAttempt;
import com.datn.quizai.attempt.repository.QuizAttemptRepository;
import com.datn.quizai.common.exception.BusinessException;
import com.datn.quizai.integrity.domain.ProctoringEvent;
import com.datn.quizai.integrity.domain.ProctoringEventType;
import com.datn.quizai.integrity.dto.ProctoringEventsRequest;
import com.datn.quizai.integrity.repository.ProctoringEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Nhận tín hiệu hành vi từ client trong lượt thi (features/12, FR-43).
 *
 * <h3>Ba chốt bắt buộc, theo ràng buộc của đặc tả</h3>
 * <ol>
 *   <li><b>Chỉ chế độ thi.</b> Lượt {@code PRACTICE} bị từ chối — luyện tập không phải chỗ để theo dõi hành
 *       vi, và ghi nhật ký ở đó là thu dữ liệu không có mục đích.</li>
 *   <li><b>Chỉ lượt của chính người gọi.</b> Không ai gửi được tín hiệu vào bài của người khác để hạ điểm tin
 *       cậy của họ.</li>
 *   <li><b>Không lưu nội dung.</b> Với {@code PASTE} chỉ giữ độ dài; nếu client gửi kèm văn bản thì văn bản
 *       đó <b>bị bỏ</b> ngay ở đây, không đi tiếp vào cơ sở dữ liệu.</li>
 * </ol>
 *
 * <h3>Chặn trên số sự kiện mỗi lượt</h3>
 * Client giả mạo được, nên nó cũng gửi được hàng triệu sự kiện. Chặn ở {@link #TOI_DA_MOI_LUOT} để một lượt
 * thi không làm phình bảng — vượt thì bỏ phần thừa và ghi log, không báo lỗi cho người dùng: họ không làm gì
 * sai, và một bài thi đang dở không nên vỡ vì chuyện này.
 */
@Service
public class ProctoringService {

    private static final Logger log = LoggerFactory.getLogger(ProctoringService.class);

    /** Số sự kiện tối đa lưu cho một lượt thi. Quá mức này thì thêm nữa cũng không đổi kết luận. */
    static final int TOI_DA_MOI_LUOT = 500;

    /** Số sự kiện tối đa trong một lần gửi — client gom thành lô. */
    public static final int TOI_DA_MOI_LO = 50;

    private final ProctoringEventRepository eventRepository;
    private final QuizAttemptRepository attemptRepository;

    public ProctoringService(ProctoringEventRepository eventRepository,
                             QuizAttemptRepository attemptRepository) {
        this.eventRepository = eventRepository;
        this.attemptRepository = attemptRepository;
    }

    /**
     * Ghi một lô tín hiệu.
     *
     * @return số sự kiện đã ghi (có thể ít hơn số gửi lên nếu đã tới chặn trên)
     */
    @Transactional
    public int ghiNhan(UUID attemptId, UUID userId, ProctoringEventsRequest request) {
        QuizAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy lượt làm bài"));

        // Chốt 2 trước chốt 1: trả 404 cho lượt của người khác thay vì tiết lộ chế độ của nó
        if (attempt.getUser() == null || !attempt.getUser().getId().equals(userId)) {
            throw BusinessException.notFound("Không tìm thấy lượt làm bài");
        }
        if (attempt.getMode() != AttemptMode.EXAM) {
            throw BusinessException.badRequest(
                    "Chỉ chế độ thi mới ghi nhận tín hiệu hành vi. Lượt luyện tập không bị theo dõi.");
        }

        long daCo = eventRepository.countByAttemptId(attemptId);
        if (daCo >= TOI_DA_MOI_LUOT) {
            log.warn("Lượt thi {} đã đạt chặn trên {} sự kiện, bỏ lô mới", attemptId, TOI_DA_MOI_LUOT);
            return 0;
        }

        int conCho = (int) (TOI_DA_MOI_LUOT - daCo);
        int daGhi = 0;

        for (var item : request.events()) {
            if (daGhi >= conCho) {
                break;
            }
            ProctoringEvent event = new ProctoringEvent();
            event.setAttemptId(attemptId);
            event.setUserId(userId);
            event.setEventType(item.type());
            event.setDetail(chiTietAnToan(item));
            // Thời điểm client báo, nhưng chặn ở hiện tại: client gửi mốc ở tương lai thì mọi thống kê theo
            // thời gian đều lệch, và đó là thứ dễ giả mạo nhất trong cả payload.
            OffsetDateTime luc = item.occurredAt() == null ? OffsetDateTime.now() : item.occurredAt();
            event.setOccurredAt(luc.isAfter(OffsetDateTime.now()) ? OffsetDateTime.now() : luc);
            eventRepository.save(event);
            daGhi++;
        }
        return daGhi;
    }

    /**
     * Dựng {@code detail} <b>chỉ từ những trường vô hại</b>.
     * <p>
     * Đây là chỗ thi hành ràng buộc "không thu dữ liệu ngoài phạm vi": kể cả khi client gửi kèm nội dung đã
     * dán, hàm này chỉ lấy độ dài. Cách khác — lưu nguyên payload client gửi — là để một thay đổi ở client
     * quyết định server lưu gì, tức mất kiểm soát chính thứ đang phải kiểm soát.
     */
    private String chiTietAnToan(ProctoringEventsRequest.Item item) {
        if (item.type() == ProctoringEventType.PASTE) {
            int doDai = Math.max(0, item.length() == null ? 0 : item.length());
            return "{\"length\":%d}".formatted(doDai);
        }
        if (item.type() == ProctoringEventType.ANSWER_TOO_FAST) {
            int giay = Math.max(0, item.seconds() == null ? 0 : item.seconds());
            return "{\"seconds\":%d}".formatted(giay);
        }
        return null;
    }
}
