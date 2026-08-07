package com.datn.quizai.recommend.repository;

import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Truy vấn gợi ý trên đồ thị (docs/features/07, FR-34 & FR-35).
 * <p>
 * <b>Ngưỡng "thế nào là yếu" nằm ở đây, không nằm trong đồ thị.</b> Cạnh {@code PRACTICED} chỉ giữ
 * sự thật đo được (đúng 4 trên 10 câu); còn "dưới 60% là yếu" là cách diễn giải. Nướng ngưỡng vào
 * cạnh thì mỗi lần chỉnh phải dựng lại toàn bộ đồ thị.
 * <p>
 * Cũng vì vậy mà <b>không lưu cạnh {@code SIMILAR_TO}</b>: "người giống tôi" tính được ngay trong
 * truy vấn từ những quiz cùng làm, mà lưu sẵn thì phải có job cập nhật và nó lỗi thời ngay sau mỗi
 * bài nộp.
 */
@Repository
public class RecommendationRepository {

    /**
     * Dưới ngưỡng này coi là yếu. 0.6 tương ứng "đúng chưa được 6/10 câu" — mức mà ở trường vẫn
     * quen gọi là chưa nắm được bài.
     */
    private static final double WEAK_THRESHOLD = 0.6;

    /**
     * Phải trả lời đủ ngần này câu trong một chủ đề thì tỷ lệ đúng mới đáng tin.
     * Sai 1 trên 1 câu là 0% nhưng không nói lên điều gì — kết luận "yếu" từ đó là võ đoán.
     */
    private static final int MIN_ANSWERS_FOR_JUDGEMENT = 3;

    /** Lấy bao nhiêu người giống mình nhất để xem họ còn làm gì. */
    private static final int PEER_LIMIT = 20;

    private final Neo4jClient neo4j;

    public RecommendationRepository(Neo4jClient neo4j) {
        this.neo4j = neo4j;
    }

    /**
     * FR-34a — quiz thuộc chủ đề đang yếu mà chưa làm.
     * <p>
     * Sắp theo <b>số câu khớp chủ đề yếu</b> trước: quiz càng trúng chỗ đang hổng càng đáng làm
     * trước. Rồi mới tới số người thật đã làm. Không có "rating" trong hệ thống nên không sắp theo
     * nó — sắp theo một con số không tồn tại thì thứ tự là ngẫu nhiên nhưng trông có vẻ có căn cứ.
     */
    public Collection<Map<String, Object>> weakTopicQuizzes(UUID userId, int limit) {
        return neo4j.query("""
                        MATCH (u:User {id: $userId})-[p:PRACTICED]->(t:Topic)<-[c:COVERS]-(q:Quiz)
                        WHERE p.total >= $minAnswers AND p.accuracy < $weakThreshold
                          AND NOT (u)-[:ATTEMPTED]->(q)
                          AND q.visibility = 'PUBLIC'
                        WITH q,
                             collect(DISTINCT t.name) AS weakTopics,
                             sum(c.questionCount) AS matchingQuestions
                        OPTIONAL MATCH (:User)-[a:ATTEMPTED]->(q)
                        RETURN q.id AS quizId, q.title AS title, weakTopics,
                               matchingQuestions, count(a) AS attemptCount
                        ORDER BY matchingQuestions DESC, attemptCount DESC
                        LIMIT $limit
                        """)
                .bindAll(Map.of(
                        "userId", userId.toString(),
                        "minAnswers", MIN_ANSWERS_FOR_JUDGEMENT,
                        "weakThreshold", WEAK_THRESHOLD,
                        "limit", limit))
                .fetch().all();
    }

    /**
     * FR-34b — lọc cộng tác: người từng làm cùng quiz với mình thì còn làm gì nữa.
     * <p>
     * Đây là chỗ đồ thị thắng hẳn bảng quan hệ: đi hai bước từ "tôi" qua "quiz đã làm" tới "người
     * khác" rồi tới "quiz họ làm" là hai phép JOIN tự thân trên `quiz_attempts` với SQL, còn Cypher
     * viết đúng như cách nghĩ.
     * <p>
     * Độ tương đồng là <b>số quiz cùng làm</b> — thô nhưng trung thực với dữ liệu đang có. Muốn tinh
     * hơn (cosine trên vector điểm) thì cần lượng người dùng lớn hơn nhiều mới có ý nghĩa.
     */
    public Collection<Map<String, Object>> peerQuizzes(UUID userId, int limit) {
        return neo4j.query("""
                        MATCH (me:User {id: $userId})-[:ATTEMPTED]->(shared:Quiz)<-[:ATTEMPTED]-(peer:User)
                        WHERE peer.id <> $userId
                        WITH peer, count(DISTINCT shared) AS similarity
                        ORDER BY similarity DESC
                        LIMIT $peerLimit
                        MATCH (peer)-[:ATTEMPTED]->(q:Quiz)
                        WHERE NOT (:User {id: $userId})-[:ATTEMPTED]->(q)
                          AND q.visibility = 'PUBLIC'
                        RETURN q.id AS quizId, q.title AS title,
                               sum(similarity) AS score, count(DISTINCT peer) AS peerCount
                        ORDER BY score DESC, peerCount DESC
                        LIMIT $limit
                        """)
                .bindAll(Map.of(
                        "userId", userId.toString(),
                        "peerLimit", PEER_LIMIT,
                        "limit", limit))
                .fetch().all();
    }

    /**
     * FR-34c — quiz thuộc chủ đề người học <b>chưa từng luyện</b>.
     * <p>
     * Nguồn thứ ba, thêm sau khi chạy thật mới thấy thiếu: hai nguồn đầu đều có thể cạn cùng
     * lúc. Người học yếu Spring Boot nhưng đã làm hết quiz Spring Boot thì nguồn "chủ đề yếu"
     * rỗng; hệ thống chỉ có vài người dùng thì nguồn "người giống bạn" cũng rỗng. Kết quả là
     * một khu Gợi ý trống trơn trong khi kho quiz vẫn còn nguyên chủ đề họ chưa đụng tới.
     * <p>
     * Nguồn này cũng giải luôn bài toán <i>cold start</i>: người mới chưa luyện chủ đề nào thì
     * mọi chủ đề đều là mới, nên có gợi ý ngay từ lần đăng nhập đầu tiên.
     */
    public Collection<Map<String, Object>> unexploredTopicQuizzes(UUID userId, int limit) {
        return neo4j.query("""
                        MATCH (q:Quiz)-[:COVERS]->(t:Topic)
                        WHERE q.visibility = 'PUBLIC'
                          AND NOT (:User {id: $userId})-[:ATTEMPTED]->(q)
                          AND NOT (:User {id: $userId})-[:PRACTICED]->(t)
                        WITH q, collect(DISTINCT t.name) AS newTopics
                        OPTIONAL MATCH (:User)-[a:ATTEMPTED]->(q)
                        RETURN q.id AS quizId, q.title AS title, newTopics,
                               count(a) AS attemptCount
                        ORDER BY attemptCount DESC, q.title
                        LIMIT $limit
                        """)
                .bindAll(Map.of("userId", userId.toString(), "limit", limit))
                .fetch().all();
    }

    /**
     * FR-35 — lộ trình học: chủ đề xếp theo mức độ yếu đo được, yếu nhất học trước.
     * <p>
     * Bản thiết kế đầu định xếp theo quan hệ tiên quyết giữa các chủ đề, nhưng không ai khai báo
     * "Vòng lặp phải học trước Mảng" — tự sinh quan hệ đó là hệ thống bịa ra kiến thức sư phạm nó
     * không có. Xếp theo tỷ lệ đúng thật vẫn là lộ trình cá nhân hoá, chỉ là thành thật về căn cứ.
     * <p>
     * Trả cả chủ đề đang khá để giao diện vẽ được bức tranh năng lực; việc chọn hiển thị bao nhiêu
     * là chuyện của tầng trên.
     */
    public Collection<Map<String, Object>> learningPath(UUID userId) {
        return neo4j.query("""
                        MATCH (u:User {id: $userId})-[p:PRACTICED]->(t:Topic)
                        OPTIONAL MATCH (t)<-[:COVERS]-(q:Quiz)
                          WHERE q.visibility = 'PUBLIC' AND NOT (u)-[:ATTEMPTED]->(q)
                        RETURN t.name AS topic,
                               p.correct AS correct,
                               p.total AS total,
                               p.accuracy AS accuracy,
                               count(DISTINCT q) AS availableQuizzes
                        ORDER BY p.accuracy ASC, p.total DESC
                        """)
                .bind(userId.toString()).to("userId")
                .fetch().all();
    }

    public static double weakThreshold() {
        return WEAK_THRESHOLD;
    }

    public static int minAnswersForJudgement() {
        return MIN_ANSWERS_FOR_JUDGEMENT;
    }
}
