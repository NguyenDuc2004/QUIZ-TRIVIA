package com.datn.quizai.recommend.service;

import com.datn.quizai.recommend.repository.AttemptGraphRepository;
import com.datn.quizai.recommend.repository.GraphWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Đọc dữ liệu từ PostgreSQL để dựng đồ thị — <b>bean riêng, tách khỏi phần ghi</b>.
 * <p>
 * Tách vì hai lý do, lý do thứ hai mới là lý do thật:
 * <ol>
 *   <li>Không giữ transaction JPA mở trong lúc gọi sang Neo4j.</li>
 *   <li><b>Việc ghi Neo4j phải nằm NGOÀI transaction để còn thử lại được.</b> Neo4j huỷ cả
 *       transaction khi phát hiện deadlock; nếu vòng lặp thử lại nằm bên trong một phương thức
 *       {@code @Transactional} thì lần thử thứ hai chạy trên một transaction đã chết và nhận
 *       "Cannot run more queries in this transaction". Muốn thử lại thì phải thoát hẳn ra ngoài.</li>
 * </ol>
 * Không gộp vào {@code GraphSyncService} được: gọi {@code this.method()} trong cùng một lớp đi
 * thẳng, không qua proxy Spring, nên {@code @Transactional} mất tác dụng.
 */
@Service
public class GraphSyncReader {

    private final AttemptGraphRepository repository;

    public GraphSyncReader(AttemptGraphRepository repository) {
        this.repository = repository;
    }

    /** Ảnh chụp mọi thứ cần ghi cho một bài làm. */
    public record Snapshot(
            UUID userId,
            UUID quizId,
            String quizTitle,
            String visibility,
            int score,
            int maxScore,
            OffsetDateTime submittedAt,
            List<GraphWriter.TopicCount> quizTopics,
            List<GraphWriter.TopicMastery> userMastery
    ) {
    }

    /**
     * Ảnh chụp danh mục quiz công khai, kèm danh sách id còn hợp lệ để gỡ nút mồ côi.
     *
     * @param topicsByQuiz chủ đề của từng quiz công khai
     * @param titles       tiêu đề để đặt lên nút Quiz
     */
    public record Catalog(
            java.util.Map<UUID, List<GraphWriter.TopicCount>> topicsByQuiz,
            java.util.Map<UUID, String> titles,
            List<UUID> validUserIds,
            List<UUID> validQuizIds
    ) {
    }

    @Transactional(readOnly = true)
    public Catalog loadCatalog() {
        java.util.Map<UUID, List<GraphWriter.TopicCount>> byQuiz = new java.util.LinkedHashMap<>();
        java.util.Map<UUID, String> titles = new java.util.LinkedHashMap<>();

        for (AttemptGraphRepository.CatalogRow row : repository.findPublicQuizCatalog()) {
            titles.put(row.getQuizId(), row.getQuizTitle());
            byQuiz.computeIfAbsent(row.getQuizId(), key -> new java.util.ArrayList<>())
                    .add(new GraphWriter.TopicCount(row.getTopic(), row.getQuestionCount()));
        }

        return new Catalog(byQuiz, titles, repository.findAllUserIds(), repository.findAllQuizIds());
    }

    /** @return null nếu bài làm không còn tồn tại */
    @Transactional(readOnly = true)
    public Snapshot load(UUID attemptId) {
        AttemptGraphRepository.AttemptRow attempt = repository.findAttemptRow(attemptId);
        if (attempt == null) {
            return null;
        }

        List<GraphWriter.TopicCount> quizTopics = repository.findQuizTopics(attempt.getQuizId()).stream()
                .map(row -> new GraphWriter.TopicCount(row.getTopic(), row.getQuestionCount()))
                .toList();

        // Tính lại năng lực trên TOÀN BỘ lịch sử, không cộng thêm phần của bài này: cộng dồn thì
        // chạy đồng bộ hai lần cho cùng một bài là số liệu nhân đôi — mà nó cố tình chạy hai lần.
        List<GraphWriter.TopicMastery> mastery = repository.findUserTopicMastery(attempt.getUserId())
                .stream()
                .map(row -> new GraphWriter.TopicMastery(
                        row.getTopic(), row.getCorrectCount(), row.getTotalCount()))
                .toList();

        return new Snapshot(
                attempt.getUserId(), attempt.getQuizId(), attempt.getQuizTitle(),
                attempt.getVisibility().name(), attempt.getTotalScore(), attempt.getMaxScore(),
                attempt.getSubmittedAt(), quizTopics, mastery);
    }
}
