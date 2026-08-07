package com.datn.quizai.recommend.service;

import com.datn.quizai.attempt.service.AttemptRegradedEvent;
import com.datn.quizai.attempt.service.AttemptSubmittedEvent;
import com.datn.quizai.recommend.repository.AttemptGraphRepository;
import com.datn.quizai.recommend.repository.GraphWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.UUID;

/**
 * Đồng bộ hành vi làm bài từ PostgreSQL sang đồ thị Neo4j (docs/features/07, FR-33).
 * <p>
 * <b>PostgreSQL là nguồn sự thật, Neo4j chỉ là view phân tích.</b> Hệ quả thực tế: đồ thị hỏng hay
 * lệch thì dựng lại được từ CSDL quan hệ, nên không cần transaction hai pha, không cần rollback —
 * chỉ cần đồng bộ <b>idempotent</b>.
 * <p>
 * <b>Neo4j chết không được kéo theo việc nộp bài.</b> Chạy nền và nuốt mọi lỗi: người học nộp bài
 * xong phải thấy điểm ngay cả khi máy chủ đồ thị đang tắt. Hậu quả xấu nhất là gợi ý cũ đi vài
 * phút — không đáng để một request nghiệp vụ trả 500.
 */
@Service
public class GraphSyncService {

    private static final Logger log = LoggerFactory.getLogger(GraphSyncService.class);

    private final AttemptGraphRepository graphSource;
    private final GraphWriter graphWriter;

    public GraphSyncService(AttemptGraphRepository graphSource, GraphWriter graphWriter) {
        this.graphSource = graphSource;
        this.graphWriter = graphWriter;
    }

    /**
     * Chạy sau khi transaction nộp bài đã commit — sớm hơn thì đọc CSDL chưa thấy bài vừa nộp.
     * <p>
     * Dùng chung bể luồng {@code aiTaskExecutor} với các tác vụ AI: bể đó cố tình chỉ một luồng để
     * không tranh hạn mức nhà cung cấp, và đồng bộ đồ thị cũng là việc nền không ai đợi nên xếp
     * cùng hàng là hợp lý — không đáng dựng thêm một bể riêng.
     */
    @Async("aiTaskExecutor")
    @TransactionalEventListener
    public void onAttemptSubmitted(AttemptSubmittedEvent event) {
        syncQuietly(event.attemptId());
    }

    /**
     * AI vừa chấm xong câu tự luận: dựng lại đồ thị với điểm thật.
     * <p>
     * {@code @EventListener} thường chứ không phải {@code @TransactionalEventListener}: sự kiện này
     * phát ra từ luồng nền, ngoài transaction, nên không có transaction nào để bám vào — dùng nhầm
     * loại thì người nhận không bao giờ chạy mà cũng chẳng báo lỗi gì.
     * <p>
     * Chạy thẳng trên luồng gọi (vốn đã là luồng nền) chứ không {@code @Async}: bể luồng AI chỉ có
     * một luồng, tự xếp việc vào hàng đợi của chính mình là thừa.
     */
    @org.springframework.context.event.EventListener
    public void onAttemptRegraded(AttemptRegradedEvent event) {
        syncQuietly(event.attemptId());
    }

    /**
     * Dựng lại toàn bộ đồ thị của một người từ lịch sử làm bài trong PostgreSQL.
     * <p>
     * Cần vì hai lẽ: dữ liệu có <b>trước</b> khi tính năng này ra đời không nằm trong đồ thị, và
     * Neo4j là view nên mất dữ liệu phải dựng lại được — đó chính là ý nghĩa của việc gọi
     * PostgreSQL là nguồn sự thật. Idempotent nên gọi bao nhiêu lần cũng cho cùng kết quả.
     *
     * @return số bài đã đồng bộ
     */
    public int rebuildForUser(UUID userId) {
        graphWriter.ensureConstraints();
        List<UUID> attemptIds = graphSource.findFinishedAttemptIds(userId);
        attemptIds.forEach(this::syncQuietly);
        log.info("Đã dựng lại đồ thị gợi ý cho người dùng {} từ {} bài làm", userId, attemptIds.size());
        return attemptIds.size();
    }

    /** Nuốt lỗi và chỉ ghi log — xem javadoc lớp về việc vì sao Neo4j hỏng không được lan ra. */
    public void syncQuietly(UUID attemptId) {
        try {
            sync(attemptId);
        } catch (Exception e) {
            log.warn("Không đồng bộ được bài {} sang đồ thị gợi ý: {}", attemptId, e.getMessage());
        }
    }

    /**
     * Đọc dữ liệu trong một transaction ngắn rồi ghi sang Neo4j.
     * <p>
     * Tách riêng để test gọi thẳng được và để lỗi Neo4j hiện ra rõ ràng thay vì bị nuốt.
     */
    @Transactional(readOnly = true)
    public void sync(UUID attemptId) {
        AttemptGraphRepository.AttemptRow attempt = graphSource.findAttemptRow(attemptId);
        if (attempt == null) {
            return;
        }

        graphWriter.upsertAttempt(
                attempt.getUserId(), attempt.getQuizId(), attempt.getQuizTitle(),
                attempt.getVisibility().name(), attempt.getTotalScore(), attempt.getMaxScore(),
                attempt.getSubmittedAt());

        List<GraphWriter.TopicCount> topics = graphSource.findQuizTopics(attempt.getQuizId()).stream()
                .map(row -> new GraphWriter.TopicCount(row.getTopic(), row.getQuestionCount()))
                .toList();
        graphWriter.replaceQuizTopics(attempt.getQuizId(), topics);

        // Tính lại năng lực trên TOÀN BỘ lịch sử, không cộng thêm phần của bài này: cộng dồn thì
        // chạy đồng bộ hai lần cho cùng một bài là số liệu nhân đôi — mà nó cố tình chạy hai lần.
        List<GraphWriter.TopicMastery> mastery = graphSource.findUserTopicMastery(attempt.getUserId())
                .stream()
                .map(row -> new GraphWriter.TopicMastery(
                        row.getTopic(), row.getCorrectCount(), row.getTotalCount()))
                .toList();
        graphWriter.replaceUserMastery(attempt.getUserId(), mastery);

        log.debug("Đã đồng bộ bài {}: {} chủ đề của quiz, {} chủ đề năng lực",
                attemptId, topics.size(), mastery.size());
    }
}
