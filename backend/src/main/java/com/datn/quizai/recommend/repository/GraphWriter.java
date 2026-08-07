package com.datn.quizai.recommend.repository;

import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Ghi đồ thị gợi ý sang Neo4j (docs/features/07).
 * <p>
 * <b>Toàn bộ dùng {@code MERGE}, không {@code CREATE}.</b> Đồng bộ phải chạy lại được bao nhiêu lần
 * cũng cho cùng một đồ thị, vì mỗi bài làm cố ý được đồng bộ <i>hai lần</i> — một lần lúc nộp, một
 * lần sau khi AI chấm xong câu tự luận (lúc nộp những câu đó còn 0 điểm nên năng lực tính ra sai).
 * Dùng {@code CREATE} thì lần thứ hai sinh ra cạnh trùng và mọi con số đếm đều gấp đôi.
 * <p>
 * <b>Viết Cypher tay thay vì map {@code @Node}:</b> đây là view phân tích chứ không phải thực thể
 * nghiệp vụ — không có ai "sửa một User trong Neo4j". Cypher hiện rõ ra cũng dễ đọc lại và dễ giải
 * thích trong báo cáo hơn là suy ngược từ annotation.
 */
@Repository
public class GraphWriter {

    private final Neo4jClient neo4j;

    public GraphWriter(Neo4jClient neo4j) {
        this.neo4j = neo4j;
    }

    /**
     * Tạo ràng buộc duy nhất cho các nút. Neo4j không có schema nên không có ràng buộc thì
     * {@code MERGE} vẫn chạy nhưng chậm dần theo số nút — ràng buộc duy nhất cũng chính là index.
     */
    public void ensureConstraints() {
        neo4j.query("CREATE CONSTRAINT user_id IF NOT EXISTS FOR (u:User) REQUIRE u.id IS UNIQUE").run();
        neo4j.query("CREATE CONSTRAINT quiz_id IF NOT EXISTS FOR (q:Quiz) REQUIRE q.id IS UNIQUE").run();
        neo4j.query("CREATE CONSTRAINT topic_name IF NOT EXISTS FOR (t:Topic) REQUIRE t.name IS UNIQUE").run();
    }

    /** Cạnh {@code (User)-[:ATTEMPTED]->(Quiz)} — giữ lần làm gần nhất, không cộng dồn. */
    public void upsertAttempt(UUID userId, UUID quizId, String quizTitle, String visibility,
                              int score, int maxScore, OffsetDateTime at) {
        neo4j.query("""
                        MERGE (u:User {id: $userId})
                        MERGE (q:Quiz {id: $quizId})
                          SET q.title = $title, q.visibility = $visibility
                        MERGE (u)-[a:ATTEMPTED]->(q)
                          SET a.score = $score,
                              a.maxScore = $maxScore,
                              a.accuracy = CASE WHEN $maxScore > 0
                                                THEN toFloat($score) / $maxScore ELSE 0.0 END,
                              a.at = $at
                        """)
                .bindAll(Map.of(
                        "userId", userId.toString(),
                        "quizId", quizId.toString(),
                        "title", quizTitle,
                        "visibility", visibility,
                        "score", score,
                        "maxScore", maxScore,
                        "at", at == null ? OffsetDateTime.now().toString() : at.toString()))
                .run();
    }

    /**
     * Cạnh {@code (Quiz)-[:COVERS]->(Topic)}.
     * <p>
     * Xoá cạnh cũ trước khi ghi: chủ quiz có thể đã bỏ bớt câu, mà {@code MERGE} thì chỉ thêm chứ
     * không biết cái gì đã biến mất. Không xoá thì quiz "phủ" mãi một chủ đề nó không còn câu nào.
     */
    public void replaceQuizTopics(UUID quizId, List<TopicCount> topics) {
        neo4j.query("MATCH (q:Quiz {id: $quizId})-[c:COVERS]->() DELETE c")
                .bind(quizId.toString()).to("quizId")
                .run();

        for (TopicCount topic : topics) {
            neo4j.query("""
                            MERGE (q:Quiz {id: $quizId})
                            MERGE (t:Topic {name: $topic})
                            MERGE (q)-[c:COVERS]->(t)
                              SET c.questionCount = $count
                            """)
                    .bindAll(Map.of(
                            "quizId", quizId.toString(),
                            "topic", topic.topic(),
                            "count", topic.count()))
                    .run();
        }
    }

    /**
     * Cạnh {@code (User)-[:PRACTICED]->(Topic)} — năng lực trên từng chủ đề.
     * <p>
     * Cũng xoá trước khi ghi, vì đây là <b>ảnh chụp toàn bộ lịch sử</b> tính lại từ đầu chứ không
     * phải phần tăng thêm. Ghi đè thẳng bằng MERGE cũng được với chủ đề còn dữ liệu, nhưng chủ đề
     * mà mọi câu vừa bị xoá khỏi ngân hàng thì sẽ nằm lại vĩnh viễn.
     */
    public void replaceUserMastery(UUID userId, List<TopicMastery> mastery) {
        neo4j.query("MATCH (:User {id: $userId})-[p:PRACTICED]->() DELETE p")
                .bind(userId.toString()).to("userId")
                .run();

        for (TopicMastery item : mastery) {
            neo4j.query("""
                            MERGE (u:User {id: $userId})
                            MERGE (t:Topic {name: $topic})
                            MERGE (u)-[p:PRACTICED]->(t)
                              SET p.correct = $correct,
                                  p.total = $total,
                                  p.accuracy = CASE WHEN $total > 0
                                                    THEN toFloat($correct) / $total ELSE 0.0 END
                            """)
                    .bindAll(Map.of(
                            "userId", userId.toString(),
                            "topic", item.topic(),
                            "correct", item.correct(),
                            "total", item.total()))
                    .run();
        }
    }

    /**
     * Tạo/cập nhật nút Quiz mà <b>không</b> cần ai làm bài — dùng khi dựng danh mục.
     * <p>
     * Gợi ý là để giới thiệu quiz người ta chưa làm; quiz chưa ai đụng tới mà không có trong
     * đồ thị thì hệ thống tự loại mất đúng thứ nó cần đề xuất.
     */
    public void upsertQuizNode(UUID quizId, String title, String visibility) {
        neo4j.query("""
                        MERGE (q:Quiz {id: $quizId})
                          SET q.title = $title, q.visibility = $visibility
                        """)
                .bindAll(Map.of(
                        "quizId", quizId.toString(),
                        "title", title,
                        "visibility", visibility))
                .run();
    }

    /**
     * Gỡ khỏi đồ thị những nút mà PostgreSQL không còn.
     * <p>
     * Đồng bộ chỉ biết <i>thêm</i>: quiz hay tài khoản bị xoá ở CSDL quan hệ thì nút của nó nằm
     * lại trong Neo4j vĩnh viễn. Hậu quả không chỉ là rác — hệ thống sẽ <b>gợi ý một quiz đã bị
     * xoá</b>, người dùng bấm vào nhận 404. Đây là mặt còn thiếu của câu "Neo4j là view":
     * view phải phản chiếu cả những gì đã biến mất.
     * <p>
     * Topic thì xoá theo kiểu khác — chỉ xoá khi không còn cạnh nào trỏ tới. Chủ đề không có
     * bảng riêng ở PostgreSQL nên không đối chiếu được, nhưng một chủ đề không còn quiz lẫn
     * người học nào gắn với nó thì hiển nhiên là tàn dư.
     *
     * @return số nút đã gỡ
     */
    public long pruneDeleted(List<UUID> validUserIds, List<UUID> validQuizIds) {
        List<String> users = validUserIds.stream().map(UUID::toString).toList();
        List<String> quizzes = validQuizIds.stream().map(UUID::toString).toList();

        long removed = 0;
        removed += deleteCount("MATCH (q:Quiz) WHERE NOT q.id IN $ids DETACH DELETE q RETURN count(q) AS c",
                "ids", quizzes);
        removed += deleteCount("MATCH (u:User) WHERE NOT u.id IN $ids DETACH DELETE u RETURN count(u) AS c",
                "ids", users);

        // Chủ đề mồ côi: không còn quiz nào phủ, không còn ai luyện
        removed += neo4j.query("MATCH (t:Topic) WHERE NOT (t)<--() DELETE t RETURN count(t) AS c")
                .fetchAs(Long.class).mappedBy((ts, rec) -> rec.get("c").asLong())
                .one().orElse(0L);

        return removed;
    }

    private long deleteCount(String cypher, String param, List<String> ids) {
        return neo4j.query(cypher)
                .bind(ids).to(param)
                .fetchAs(Long.class).mappedBy((ts, rec) -> rec.get("c").asLong())
                .one().orElse(0L);
    }

    /** Số nút và cạnh hiện có — để endpoint chẩn đoán nói được đồ thị đã có gì chưa. */
    public Map<String, Object> stats() {
        return neo4j.query("""
                        MATCH (u:User) WITH count(u) AS users
                        MATCH (q:Quiz) WITH users, count(q) AS quizzes
                        MATCH (t:Topic) WITH users, quizzes, count(t) AS topics
                        OPTIONAL MATCH ()-[a:ATTEMPTED]->() WITH users, quizzes, topics, count(a) AS attempts
                        OPTIONAL MATCH ()-[p:PRACTICED]->()
                        RETURN users, quizzes, topics, attempts, count(p) AS practiced
                        """)
                .fetch().one()
                .orElse(Map.of("users", 0, "quizzes", 0, "topics", 0, "attempts", 0, "practiced", 0));
    }

    public record TopicCount(String topic, long count) {
    }

    public record TopicMastery(String topic, long correct, long total) {
    }
}
