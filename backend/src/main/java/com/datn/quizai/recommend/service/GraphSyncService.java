package com.datn.quizai.recommend.service;

import com.datn.quizai.attempt.service.AttemptRegradedEvent;
import com.datn.quizai.attempt.service.AttemptSubmittedEvent;
import com.datn.quizai.recommend.repository.AttemptGraphRepository;
import com.datn.quizai.recommend.repository.GraphWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
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
    private final GraphSyncReader reader;

    public GraphSyncService(AttemptGraphRepository graphSource, GraphWriter graphWriter,
                            GraphSyncReader reader) {
        this.graphSource = graphSource;
        this.graphWriter = graphWriter;
        this.reader = reader;
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
        // Dựng lại cả danh mục: không có nó thì dù biết người này yếu chủ đề gì cũng không có quiz
        // nào trong đồ thị để mà gợi ý.
        syncPublicCatalog();
        List<UUID> attemptIds = graphSource.findFinishedAttemptIds(userId);
        attemptIds.forEach(this::syncQuietly);
        log.info("Đã dựng lại đồ thị gợi ý cho người dùng {} từ {} bài làm", userId, attemptIds.size());
        return attemptIds.size();
    }

    /**
     * Đưa toàn bộ quiz công khai và chủ đề của chúng vào đồ thị.
     * <p>
     * Tách khỏi đồng bộ theo bài làm vì đây là <b>danh mục</b>, không phải hành vi: việc một
     * quiz phủ chủ đề nào là thuộc tính của chính nó. Không có bước này thì quiz chưa ai làm
     * sẽ không nằm trong đồ thị và không bao giờ được gợi ý — mà gợi ý đúng là để giới thiệu
     * quiz người ta <i>chưa</i> làm, nên hệ thống tự loại mất đúng thứ nó cần đề xuất.
     *
     * @return số quiz đã đưa vào
     */
    public int syncPublicCatalog() {
        GraphSyncReader.Catalog catalog = reader.loadCatalog();

        // MỖI quiz thử lại riêng, không bọc cả vòng lặp trong một lần thử.
        //
        // Bọc cả vòng thì một deadlock ở quiz thứ 50 làm chạy lại từ quiz thứ nhất — vô ích vì 49 cái
        // trước đã ghi xong, và làm tăng đúng thứ gây deadlock: thời gian giữ khoá.
        catalog.topicsByQuiz().forEach((quizId, topics) -> thuLaiKhiDungDo(
                "quiz " + quizId,
                () -> {
                    graphWriter.upsertQuizNode(quizId, catalog.titles().get(quizId), "PUBLIC");
                    graphWriter.replaceQuizTopics(quizId, topics);
                }));

        // Gỡ nút của quiz/tài khoản đã bị xoá ở PostgreSQL. Không có bước này thì đồ thị chỉ lớn
        // lên mãi, và tệ hơn: hệ thống gợi ý một quiz đã biến mất, người dùng bấm vào nhận 404.
        long[] soGo = new long[1];
        thuLaiKhiDungDo("gỡ nút cũ",
                () -> soGo[0] = graphWriter.pruneDeleted(catalog.validUserIds(), catalog.validQuizIds()));
        long pruned = soGo[0];

        log.info("Đồ thị gợi ý: {} quiz công khai, gỡ {} nút không còn trong PostgreSQL",
                catalog.topicsByQuiz().size(), pruned);
        return catalog.topicsByQuiz().size();
    }

    /** Nuốt lỗi và chỉ ghi log — xem javadoc lớp về việc vì sao Neo4j hỏng không được lan ra. */
    public void syncQuietly(UUID attemptId) {
        try {
            sync(attemptId);
        } catch (Exception e) {
            log.warn("Không đồng bộ được bài {} sang đồ thị gợi ý: {}", attemptId, e.getMessage());
        }
    }

    /** Số lần thử lại khi hai luồng đồng bộ đụng nhau — xem {@link #sync}. */
    private static final int DEADLOCK_RETRIES = 3;

    /**
     * Đồng bộ một bài: đọc PostgreSQL một lần, rồi ghi sang Neo4j với thử lại khi vấp khoá.
     * <p>
     * Hai luồng cùng đồng bộ hai bài <i>khác nhau</i> nhưng <i>cùng một quiz</i> sẽ giành khoá trên
     * cùng nút Quiz, Neo4j phát hiện deadlock rồi huỷ một bên. Chuyện bình thường với CSDL đồ thị —
     * cách xử lý đúng là thử lại, không phải né bằng cách khoá to hơn.
     * <p>
     * <b>Phương thức này cố tình KHÔNG {@code @Transactional}.</b> Neo4j huỷ cả transaction khi
     * deadlock; vòng lặp thử lại nằm trong một transaction đã chết sẽ nhận "Cannot run more queries
     * in this transaction". Phần đọc nằm ở {@link GraphSyncReader} — bean riêng, vì gọi
     * {@code this.method()} trong cùng lớp không qua proxy nên {@code @Transactional} sẽ mất tác dụng.
     * <p>
     * Thử lại an toàn <b>chính vì đồng bộ idempotent</b>. Đây là lần thứ hai tính chất đó trả công:
     * lần đầu là để chạy hai lượt cho mỗi bài (lúc nộp và sau khi AI chấm).
     */
    public void sync(UUID attemptId) {
        GraphSyncReader.Snapshot snapshot = reader.load(attemptId);
        if (snapshot == null) {
            return;
        }

        thuLaiKhiDungDo("bài " + attemptId, () -> write(snapshot));
    }

    /**
     * Chạy một phép ghi Neo4j, thử lại khi đụng độ với luồng khác.
     *
     * <h4>Vì sao phải là một chỗ dùng chung, không phải một vòng lặp trong {@link #sync}</h4>
     * Bản đầu chỉ bọc đường đồng bộ theo bài làm. Đường {@link #syncPublicCatalog} ghi vào <b>cùng những
     * nút Quiz đó</b> nhưng không có thử lại, nên hai đường chạy song song thì đường không được bọc đổ —
     * và nó đổ ra ngoài thành lỗi 500 cho người dùng. Đã bắt được đúng chuyện này ở
     * {@code shouldOrderPathByWeakestFirst}: deadlock qua {@code rebuildForUser → syncPublicCatalog}.
     *
     * <h4>Hai kiểu đụng độ, cùng cách chữa</h4>
     * <ul>
     *   <li>{@code TransientDataAccessException}: Neo4j phát hiện deadlock rồi huỷ một bên.</li>
     *   <li>{@code DataIntegrityViolationException}: hai luồng cùng {@code MERGE} một nút chưa tồn tại,
     *       cả hai cùng quyết định tạo, và ràng buộc duy nhất chặn kẻ tới sau. {@code MERGE} nghe như
     *       "tạo nếu chưa có" nhưng <b>không nguyên tử</b> với luồng khác — chỗ này rất dễ tin nhầm.</li>
     * </ul>
     * Thử lại an toàn <b>chính vì mọi phép ghi ở đây idempotent</b>.
     */
    private void thuLaiKhiDungDo(String moTa, Runnable phepGhi) {
        for (int lan = 1; ; lan++) {
            try {
                phepGhi.run();
                return;
            } catch (org.springframework.dao.TransientDataAccessException
                     | org.springframework.dao.DataIntegrityViolationException e) {
                if (lan >= DEADLOCK_RETRIES) {
                    throw e;
                }
                log.debug("Đụng độ ghi Neo4j khi đồng bộ {} ({}), thử lại lần {}",
                        moTa, e.getClass().getSimpleName(), lan + 1);
                sleepBriefly(lan);
            }
        }
    }

    private void write(GraphSyncReader.Snapshot snapshot) {
        graphWriter.upsertAttempt(
                snapshot.userId(), snapshot.quizId(), snapshot.quizTitle(), snapshot.visibility(),
                snapshot.score(), snapshot.maxScore(), snapshot.submittedAt());
        graphWriter.replaceQuizTopics(snapshot.quizId(), snapshot.quizTopics());
        graphWriter.replaceUserMastery(snapshot.userId(), snapshot.userMastery());
    }

    /** Nghỉ rất ngắn và tăng dần, đủ để luồng kia buông khoá. */
    private void sleepBriefly(int attempt) {
        try {
            Thread.sleep(50L * attempt);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Bị ngắt khi chờ thử lại đồng bộ đồ thị", e);
        }
    }
}
