package com.datn.quizai.recommend.service;

import com.datn.quizai.recommend.dto.LearningPathResponse;
import com.datn.quizai.recommend.dto.RecommendationsResponse;
import com.datn.quizai.recommend.dto.RecommendedQuizResponse;
import com.datn.quizai.recommend.dto.TopicMasteryResponse;
import com.datn.quizai.quiz.repository.QuizRepository;
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
import java.util.stream.Collectors;

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
    private final QuizRepository quizRepository;

    public RecommendationService(RecommendationRepository repository,
                                 QuizRepository quizRepository) {
        this.repository = repository;
        this.quizRepository = quizRepository;
    }

    /**
     * Danh sách quiz gợi ý, trộn hai nguồn.
     * <p>
     * Ưu tiên quiz chạm chủ đề đang yếu; còn chỗ thì lấp bằng quiz mà những người học giống mình đã
     * làm. Chỉ dùng một nguồn thì hỏng theo hai kiểu khác nhau: chỉ theo chủ đề yếu thì người mới
     * (chưa sai gì) không có gợi ý nào, còn chỉ theo cộng tác thì gợi ý trôi theo đám đông mà không
     * liên quan gì tới chỗ người này đang hổng.
     */
    public RecommendationsResponse recommendQuizzes(UUID userId, int limit) {
        GraphHealth health = new GraphHealth();
        List<RecommendedQuizResponse> items = merge(userId, limit, health);
        return RecommendationsResponse.of(items, noteForEmpty(health));
    }

    /**
     * Vì sao danh sách rỗng — ba tình huống, ba việc người dùng nên làm khác nhau.
     * <p>
     * Chỉ chạy khi danh sách rỗng, nên câu đếm quiz không tốn gì trong trường hợp thường.
     */
    private String noteForEmpty(GraphHealth health) {
        if (health.failed) {
            // Nói thật là chưa lấy được, đừng để người dùng tưởng kho quiz trống rỗng
            return "Chưa lấy được gợi ý lúc này. Thử lại sau ít phút.";
        }
        if (quizRepository.countPublicQuizzesWithQuestions() == 0) {
            return "Chưa có quiz công khai nào có câu hỏi để gợi ý.";
        }
        return "Bạn đã làm hết quiz công khai đang có. Quiz mới xuất bản sẽ xuất hiện ở đây.";
    }

    /** Đồ thị có hỏng trong lượt truy vấn này không — quyết định câu giải thích khi rỗng. */
    private static final class GraphHealth {
        private boolean failed;
    }

    private List<RecommendedQuizResponse> merge(UUID userId, int limit, GraphHealth health) {
        Collected first = collect(userId, limit, health);

        // Nút quiz đã xoá bị loại ở bước lấy dữ liệu hiển thị, mà lúc đó danh sách đã cắt theo
        // limit — nên nút rác "ăn" mất chỗ và người dùng nhận ít gợi ý hơn, có khi trống trơn dù kho
        // quiz còn nguyên. Hỏi lại đồ thị với limit rộng hơn đúng bằng số bị loại.
        //
        // Chỉ chạy khi THẬT SỰ có nút bị loại, không chạy khi đơn giản là kho ít quiz — nếu không thì
        // mỗi lượt gợi ý trên kho nhỏ đều tốn thêm một vòng truy vấn đồ thị mà không đổi được gì.
        if (first.dropped() > 0) {
            Collected wider = collect(userId, limit + first.dropped(), health);
            List<RecommendedQuizResponse> items = wider.items();
            return items.size() > limit ? List.copyOf(items.subList(0, limit)) : items;
        }

        return first.items();
    }

    /** Kết quả một lượt trộn: danh sách đã làm tươi, kèm số nút bị loại vì quiz không còn tồn tại. */
    private record Collected(List<RecommendedQuizResponse> items, int dropped) {
    }

    private Collected collect(UUID userId, int limit, GraphHealth health) {
        Map<UUID, RecommendedQuizResponse> merged = new LinkedHashMap<>();

        int weakSlots = Math.min(limit, WEAK_TOPIC_SHARE);
        for (Map<String, Object> row : safely(health, () -> repository.weakTopicQuizzes(userId, weakSlots))) {
            RecommendedQuizResponse item = fromWeakTopic(row);
            merged.put(item.quizId(), item);
        }

        if (merged.size() < limit) {
            for (Map<String, Object> row : safely(health, () -> repository.peerQuizzes(userId, limit))) {
                RecommendedQuizResponse item = fromPeers(row);
                // Quiz đã lọt vào vì chủ đề yếu thì giữ nguyên lý do đó — nó cụ thể hơn
                merged.putIfAbsent(item.quizId(), item);
                if (merged.size() >= limit) {
                    break;
                }
            }
        }

        // Vẫn chưa đủ: đề xuất chủ đề chưa thử. Không có nhánh này thì người đã làm hết quiz thuộc
        // chủ đề mình yếu sẽ thấy khu Gợi ý trống trơn, dù kho quiz còn nguyên chủ đề khác.
        if (merged.size() < limit) {
            for (Map<String, Object> row : safely(health, () -> repository.unexploredTopicQuizzes(userId, limit))) {
                RecommendedQuizResponse item = fromNewTopic(row);
                merged.putIfAbsent(item.quizId(), item);
                if (merged.size() >= limit) {
                    break;
                }
            }
        }

        List<RecommendedQuizResponse> items = withFreshDisplayData(merged.values());
        return new Collected(items, merged.size() - items.size());
    }

    /**
     * Thay tiêu đề và ảnh bìa bằng bản đọc từ PostgreSQL, <b>một truy vấn cho cả danh sách</b>.
     * <p>
     * Neo4j giữ quan hệ, không giữ thứ để hiển thị. Nhân bản ảnh bìa sang đồ thị thì mỗi lần chủ
     * quiz đổi ảnh, thẻ gợi ý lại trỏ vào file cũ đã bị xoá — mà đồ thị chỉ được đồng bộ khi có bài
     * nộp mới, nên nó có thể cũ rất lâu.
     * <p>
     * Quiz không còn trong PostgreSQL bị <b>loại khỏi danh sách</b>: nút rác trong đồ thị không được
     * phép hiện thành một thẻ bấm vào là 404.
     */
    private List<RecommendedQuizResponse> withFreshDisplayData(
            Collection<RecommendedQuizResponse> items) {
        if (items.isEmpty()) {
            return List.of();
        }

        Map<UUID, QuizRepository.QuizCardRow> cards = quizRepository
                .findCardsByIds(items.stream().map(RecommendedQuizResponse::quizId).toList())
                .stream()
                .collect(Collectors.toMap(QuizRepository.QuizCardRow::getId, row -> row));

        return items.stream()
                .filter(item -> cards.containsKey(item.quizId()))
                .map(item -> {
                    QuizRepository.QuizCardRow card = cards.get(item.quizId());
                    return item.withDisplayData(card.getTitle(), card.getThumbnailUrl());
                })
                .toList();
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
                null,   // ảnh bìa nạp từ PostgreSQL ở withFreshDisplayData()
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
                null,   // ảnh bìa nạp từ PostgreSQL ở withFreshDisplayData()
                RecommendedQuizResponse.RecommendationSource.SIMILAR_LEARNERS,
                peers + " người học giống bạn đã làm quiz này",
                List.of(),
                peers,
                0);
    }

    private RecommendedQuizResponse fromNewTopic(Map<String, Object> row) {
        @SuppressWarnings("unchecked")
        List<String> topics = (List<String>) row.getOrDefault("newTopics", List.of());
        return new RecommendedQuizResponse(
                UUID.fromString((String) row.get("quizId")),
                (String) row.get("title"),
                null,   // ảnh bìa nạp từ PostgreSQL ở withFreshDisplayData()
                RecommendedQuizResponse.RecommendationSource.NEW_TOPIC,
                "Chủ đề bạn chưa thử: " + String.join(", ", topics),
                List.of(),
                0,
                asLong(row.get("attemptCount")));
    }

    /** Xem javadoc lớp: đồ thị hỏng thì gợi ý rỗng, không kéo sập trang. */
    private Collection<Map<String, Object>> safely(
            java.util.function.Supplier<Collection<Map<String, Object>>> query) {
        return safely(null, query);
    }

    /**
     * Như trên, nhưng <b>ghi nhận</b> việc đồ thị hỏng vào {@code health}.
     * <p>
     * Nuốt lỗi rồi trả rỗng là đúng (gợi ý không được kéo sập trang chủ), nhưng nuốt xong <i>im
     * lặng</i> thì người dùng nhận đúng một màn hình trống giống như khi đã làm hết quiz — hai
     * chuyện hoàn toàn khác nhau. Cờ này để câu giải thích nói đúng chuyện đang xảy ra.
     */
    private Collection<Map<String, Object>> safely(
            GraphHealth health,
            java.util.function.Supplier<Collection<Map<String, Object>>> query) {
        try {
            return query.get();
        } catch (Exception e) {
            log.warn("Không truy vấn được đồ thị gợi ý: {}", e.getMessage());
            if (health != null) {
                health.failed = true;
            }
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
