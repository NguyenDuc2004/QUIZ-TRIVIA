package com.datn.quizai.recommend.service;

import com.datn.quizai.recommend.dto.LearningPathResponse;
import com.datn.quizai.recommend.dto.RecommendedQuizResponse;
import com.datn.quizai.recommend.dto.TopicMasteryResponse;
import com.datn.quizai.recommend.repository.RecommendationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Gợi ý quiz và lộ trình học từ đồ thị (docs/features/07, FR-34 & FR-35).
 * <p>
 * <b>Đồ thị hỏng thì trả rỗng, không trả 500.</b> Gợi ý là tính năng phụ trợ nằm trên trang chủ;
 * Neo4j tắt mà kéo cả trang chủ sập là đánh đổi sai. Mọi lỗi bị nuốt và biến thành một danh sách
 * rỗng kèm ghi chú nói thật là chưa lấy được.
 */
@Service
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);

    /** Trộn hai nguồn nhưng ưu tiên chủ đề yếu: sửa chỗ hổng đáng hơn là xem người khác học gì. */
    private static final int WEAK_TOPIC_SHARE = 6;

    private final RecommendationRepository repository;

    public RecommendationService(RecommendationRepository repository) {
        this.repository = repository;
    }

    /**
     * Danh sách quiz gợi ý, trộn hai nguồn.
     * <p>
     * Ưu tiên quiz chạm chủ đề đang yếu; còn chỗ thì lấp bằng quiz mà những người học giống mình đã
     * làm. Chỉ dùng một nguồn thì hỏng theo hai kiểu khác nhau: chỉ theo chủ đề yếu thì người mới
     * (chưa sai gì) không có gợi ý nào, còn chỉ theo cộng tác thì gợi ý trôi theo đám đông mà không
     * liên quan gì tới chỗ người này đang hổng.
     */
    public List<RecommendedQuizResponse> recommendQuizzes(UUID userId, int limit) {
        Map<UUID, RecommendedQuizResponse> merged = new LinkedHashMap<>();

        int weakSlots = Math.min(limit, WEAK_TOPIC_SHARE);
        for (Map<String, Object> row : safely(() -> repository.weakTopicQuizzes(userId, weakSlots))) {
            RecommendedQuizResponse item = fromWeakTopic(row);
            merged.put(item.quizId(), item);
        }

        if (merged.size() < limit) {
            for (Map<String, Object> row : safely(() -> repository.peerQuizzes(userId, limit))) {
                RecommendedQuizResponse item = fromPeers(row);
                // Quiz đã lọt vào vì chủ đề yếu thì giữ nguyên lý do đó — nó cụ thể hơn
                merged.putIfAbsent(item.quizId(), item);
                if (merged.size() >= limit) {
                    break;
                }
            }
        }

        return List.copyOf(merged.values());
    }

    /** Lộ trình học: chủ đề xếp theo mức độ yếu đo được (FR-35). */
    public LearningPathResponse learningPath(UUID userId) {
        List<TopicMasteryResponse> topics = new ArrayList<>();
        for (Map<String, Object> row : safely(() -> repository.learningPath(userId))) {
            long total = asLong(row.get("total"));
            double accuracy = asDouble(row.get("accuracy"));
            topics.add(new TopicMasteryResponse(
                    (String) row.get("topic"),
                    asLong(row.get("correct")),
                    total,
                    accuracy,
                    isWeak(accuracy, total),
                    asLong(row.get("availableQuizzes"))));
        }

        long weakCount = topics.stream().filter(TopicMasteryResponse::weak).count();
        return new LearningPathResponse(topics, weakCount, noteFor(topics, weakCount));
    }

    // ------------------------------------------------------------------ nội bộ

    /**
     * Cùng ngưỡng với truy vấn Cypher.
     * <p>
     * Một chủ đề mới trả lời một hai câu thì tỷ lệ đúng chưa nói lên gì — gắn nhãn "yếu" từ đó là
     * võ đoán, và người học đọc xong sẽ mất tin vào cả những nhãn đúng.
     */
    private boolean isWeak(double accuracy, long total) {
        return total >= RecommendationRepository.minAnswersForJudgement()
                && accuracy < RecommendationRepository.weakThreshold();
    }

    /** Danh sách rỗng phải nói được vì sao rỗng, nếu không người dùng tưởng hệ thống hỏng. */
    private String noteFor(List<TopicMasteryResponse> topics, long weakCount) {
        if (topics.isEmpty()) {
            return "Bạn chưa làm bài nào có gắn chủ đề. Làm vài quiz để hệ thống hiểu bạn đang mạnh yếu ở đâu.";
        }
        boolean enoughData = topics.stream()
                .anyMatch(t -> t.total() >= RecommendationRepository.minAnswersForJudgement());
        if (!enoughData) {
            return "Chưa đủ dữ liệu để đánh giá — cần ít nhất "
                    + RecommendationRepository.minAnswersForJudgement()
                    + " câu trong một chủ đề thì tỷ lệ đúng mới đáng tin.";
        }
        if (weakCount == 0) {
            return "Bạn đang nắm khá tất cả chủ đề đã học. Thử chủ đề mới xem sao.";
        }
        return null;
    }

    private RecommendedQuizResponse fromWeakTopic(Map<String, Object> row) {
        @SuppressWarnings("unchecked")
        List<String> weakTopics = (List<String>) row.getOrDefault("weakTopics", List.of());
        return new RecommendedQuizResponse(
                UUID.fromString((String) row.get("quizId")),
                (String) row.get("title"),
                RecommendedQuizResponse.RecommendationSource.WEAK_TOPIC,
                "Ôn lại " + String.join(", ", weakTopics) + " — bạn đang làm sai nhiều ở đây",
                weakTopics,
                0,
                asLong(row.get("attemptCount")));
    }

    private RecommendedQuizResponse fromPeers(Map<String, Object> row) {
        long peers = asLong(row.get("peerCount"));
        return new RecommendedQuizResponse(
                UUID.fromString((String) row.get("quizId")),
                (String) row.get("title"),
                RecommendedQuizResponse.RecommendationSource.SIMILAR_LEARNERS,
                peers + " người học giống bạn đã làm quiz này",
                List.of(),
                peers,
                0);
    }

    /** Xem javadoc lớp: đồ thị hỏng thì gợi ý rỗng, không kéo sập trang. */
    private Collection<Map<String, Object>> safely(java.util.function.Supplier<Collection<Map<String, Object>>> query) {
        try {
            return query.get();
        } catch (Exception e) {
            log.warn("Không truy vấn được đồ thị gợi ý: {}", e.getMessage());
            return List.of();
        }
    }

    private static long asLong(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private static double asDouble(Object value) {
        return value instanceof Number number ? number.doubleValue() : 0.0;
    }
}
