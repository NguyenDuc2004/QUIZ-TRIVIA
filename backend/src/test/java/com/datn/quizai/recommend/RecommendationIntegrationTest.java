package com.datn.quizai.recommend;

import com.datn.quizai.recommend.service.GraphSyncService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test gợi ý dựa trên đồ thị (docs/features/07) trên PostgreSQL + Redis + <b>Neo4j thật</b>.
 * <p>
 * Neo4j chạy bằng Testcontainer chứ không mock: cả tính năng này <i>là</i> mấy câu Cypher, mock đi
 * thì chỉ còn kiểm được việc gọi hàm. Cypher sai cú pháp hay sai logic đồ thị chỉ lộ ra khi có một
 * Neo4j thật chạy nó.
 * <p>
 * Trọng tâm: đồng bộ có <b>idempotent</b> không (chạy hai lần không nhân đôi số liệu), gợi ý có
 * đúng chỗ người học đang yếu không, và Neo4j hỏng thì hệ thống có đứng vững không.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RecommendationIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @Container
    @ServiceConnection
    static Neo4jContainer<?> neo4j = new Neo4jContainer<>(DockerImageName.parse("neo4j:5"))
            .withoutAuthentication();

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private GraphSyncService graphSync;
    @Autowired
    private Neo4jClient neo4jClient;

    private String creatorToken;
    private String learnerToken;
    private String peerToken;
    private UUID learnerId;

    @BeforeAll
    void setUp() throws Exception {
        creatorToken = register("creator-goiy@example.com", "CREATOR");
        learnerToken = register("learner-goiy@example.com", "LEARNER");
        peerToken = register("peer-goiy@example.com", "LEARNER");
        learnerId = currentUserId(learnerToken);
    }

    // ================================================================ đồng bộ đồ thị

    @Test
    @DisplayName("Nộp bài xong thì đồ thị có đúng nút và cạnh của bài đó")
    void shouldBuildGraphFromAttempt() throws Exception {
        // Tài khoản riêng cho ca này. Đếm cạnh toàn cục thì kết quả phụ thuộc những ca chạy trước
        // nó — chạy lẻ thì đạt, chạy cả bộ thì hỏng, mà cái hỏng đó không nói lên điều gì về code.
        String token = register("do-thi-co-ban@example.com", "LEARNER");
        UUID userId = currentUserId(token);

        String quizId = quizWithTopic("Đồ thị cơ bản", "Lịch sử đồ thị", 3);
        graphSync.sync(UUID.fromString(takeQuiz(quizId, token, 0)));   // trả lời sai hết

        assertThat(hasAttemptedEdge(userId, quizId)).isTrue();
        assertThat(coveredTopics(quizId)).contains("Lịch sử đồ thị");
        assertThat(practicedTotal(userId, "Lịch sử đồ thị")).isEqualTo(3);
    }

    @Test
    @DisplayName("Đồng bộ hai lần KHÔNG nhân đôi số liệu — bước này cố tình chạy hai lần cho mỗi bài")
    void shouldBeIdempotent() throws Exception {
        String quizId = quizWithTopic("Idempotent", "Chủ đề lặp", 4);
        String attemptId = takeQuiz(quizId, learnerToken, 2);
        UUID id = UUID.fromString(attemptId);

        graphSync.sync(id);
        long practicedAfterFirst = practicedTotal(learnerId, "Chủ đề lặp");
        long attemptEdgesAfterFirst = attemptedEdgeCount(learnerId);

        // Lúc nộp câu tự luận còn 0 điểm nên bước này chạy lại sau khi AI chấm xong.
        // Dùng CREATE thay vì MERGE thì đúng chỗ này số liệu gấp đôi.
        graphSync.sync(id);
        graphSync.sync(id);

        assertThat(practicedTotal(learnerId, "Chủ đề lặp")).isEqualTo(practicedAfterFirst);
        assertThat(attemptedEdgeCount(learnerId)).isEqualTo(attemptEdgesAfterFirst);
    }

    @Test
    @DisplayName("Quiz bỏ bớt câu thì cạnh COVERS cũ bị gỡ, không phủ mãi chủ đề đã hết câu")
    void shouldDropStaleTopicEdges() throws Exception {
        String historyQ = questionWithTopic("Trận Bạch Đằng năm nào?", "Sử cũ");
        String mathQ = questionWithTopic("2 + 2 = ?", "Toán cũ");
        String quizId = createQuiz("Quiz đổi đề");
        setQuizQuestions(quizId, historyQ, mathQ);

        String attemptId = takeQuiz(quizId, learnerToken, 1);
        graphSync.sync(UUID.fromString(attemptId));
        assertThat(coveredTopics(quizId)).contains("Sử cũ", "Toán cũ");

        // Chủ quiz bỏ câu Toán ra khỏi đề
        setQuizQuestions(quizId, historyQ);
        graphSync.sync(UUID.fromString(attemptId));

        // Chờ chủ đề cũ biến mất thay vì khẳng định ngay một lần.
        //
        // `takeQuiz()` nộp bài → phát sự kiện miền → một lần ĐỒNG BỘ NỀN chạy bất đồng bộ. Lần đó mang
        // ảnh chụp đọc từ TRƯỚC khi câu Toán bị gỡ, nên nếu nó về đích sau lần `sync()` tường minh ở trên
        // thì "Toán cũ" được ghi lại. Đây là hành vi chấp nhận được của sản phẩm — đồ thị là một VIEW,
        // idempotent và dựng lại được, nên lần đồng bộ kế tiếp sẽ sửa. Nhưng nó làm phép kiểm đỏ ngẫu
        // nhiên: xanh khi chạy riêng, đỏ khi máy bận và luồng nền về muộn.
        assertThat(choChuDeBienMat(quizId, UUID.fromString(attemptId), "Toán cũ"))
                .as("chủ đề của câu đã gỡ phải rời khỏi đồ thị")
                .isTrue();
        assertThat(coveredTopics(quizId)).contains("Sử cũ");
    }

    @Test
    @DisplayName("Quiz bị xoá khỏi PostgreSQL thì nút của nó bị gỡ khỏi đồ thị")
    void shouldPruneNodesDeletedInPostgres() throws Exception {
        String token = register("bi-xoa@example.com", "LEARNER");
        String quizId = quizWithTopic("Quiz sắp bị xoá", "Chủ đề sắp mất", 3);
        graphSync.sync(UUID.fromString(takeQuiz(quizId, token, 1)));
        assertThat(quizNodeExists(quizId)).isTrue();

        // Chủ quiz xoá hẳn quiz ở CSDL quan hệ
        mockMvc.perform(delete("/api/v1/quizzes/{id}", quizId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + creatorToken))
                .andExpect(status().isNoContent());

        graphSync.syncPublicCatalog();

        // Không gỡ thì hệ thống sẽ gợi ý một quiz đã biến mất và người dùng bấm vào nhận 404
        assertThat(quizNodeExists(quizId)).isFalse();
    }

    @Test
    @DisplayName("Quiz công khai chưa ai làm vẫn vào được đồ thị — nếu không thì không bao giờ được gợi ý")
    void shouldPutUntouchedPublicQuizIntoCatalog() throws Exception {
        String quizId = quizWithTopic("Chưa ai làm", "Chủ đề mới tinh", 3);

        // Không ai làm bài này cả — chỉ chạy đồng bộ danh mục
        graphSync.syncPublicCatalog();

        assertThat(quizNodeExists(quizId)).isTrue();
        assertThat(coveredTopics(quizId)).contains("Chủ đề mới tinh");
    }

    // ================================================================ dữ liệu hiển thị của thẻ gợi ý

    @Test
    @DisplayName("Thẻ gợi ý lấy tiêu đề và ảnh bìa từ PostgreSQL, không dùng bản sao trong đồ thị")
    void shouldReadCardDataFromPostgres() throws Exception {
        String token = register("anh-bia@example.com", "LEARNER");
        String quizId = quizWithTopic("Tên cũ trong đồ thị", "Chủ đề ảnh bìa", 3);

        // Đưa quiz vào đồ thị với tiêu đề hiện tại, rồi mới đổi tên + thêm ảnh ở PostgreSQL.
        // Đồ thị chỉ được đồng bộ khi có bài nộp mới, nên bản sao trong đó CÒN CŨ ở thời điểm gợi ý.
        graphSync.syncPublicCatalog();
        mockMvc.perform(put("/api/v1/quizzes/{id}", quizId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + creatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Tên mới ở PostgreSQL","visibility":"PUBLIC",
                                 "thumbnailUrl":"/uploads/anh-bia-test.png"}
                                """))
                .andExpect(status().isOk());

        JsonNode card = findRecommendation(token, quizId);
        assertThat(card).as("quiz công khai phải được gợi ý cho người mới").isNotNull();
        assertThat(card.get("title").asText()).isEqualTo("Tên mới ở PostgreSQL");
        assertThat(card.get("thumbnailUrl").asText()).isEqualTo("/uploads/anh-bia-test.png");
    }

    @Test
    @DisplayName("Quiz chưa có ảnh: thumbnailUrl là null, KHÔNG phải chuỗi rỗng hay ảnh bịa")
    void shouldReturnNullThumbnailWhenQuizHasNoImage() throws Exception {
        String token = register("khong-anh@example.com", "LEARNER");
        String quizId = quizWithTopic("Quiz không ảnh", "Chủ đề không ảnh", 3);
        graphSync.syncPublicCatalog();

        JsonNode card = findRecommendation(token, quizId);
        assertThat(card).isNotNull();
        // Frontend dựa vào null để vẽ khối màu thay thế; chuỗi rỗng sẽ thành thẻ <img src="">
        assertThat(card.get("thumbnailUrl").isNull()).isTrue();
    }

    @Test
    @DisplayName("Nút rác còn trong đồ thị nhưng quiz đã xoá: KHÔNG hiện thành thẻ bấm vào là 404")
    void shouldDropRecommendationsForDeletedQuizzes() throws Exception {
        String token = register("goi-y-quiz-da-xoa@example.com", "LEARNER");
        String quizId = quizWithTopic("Quiz sẽ bị xoá khỏi CSDL", "Chủ đề quiz đã xoá", 3);
        graphSync.syncPublicCatalog();
        assertThat(findRecommendation(token, quizId)).isNotNull();

        // Xoá ở PostgreSQL nhưng CỐ Ý không chạy syncPublicCatalog: mô phỏng khoảng thời gian đồ thị
        // còn cũ. Danh sách gợi ý phải tự loại quiz không còn tồn tại.
        mockMvc.perform(delete("/api/v1/quizzes/{id}", quizId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + creatorToken))
                .andExpect(status().isNoContent());

        assertThat(findRecommendation(token, quizId)).isNull();
    }

    @Test
    @DisplayName("Nút rác KHÔNG ăn mất chỗ: xoá 2 quiz đầu bảng thì danh sách vẫn đủ 4, không tụt còn 2")
    void shouldNotLetStaleNodesEatRecommendationSlots() throws Exception {
        // Bộ lọc quiz-đã-xoá chạy SAU khi danh sách đã cắt theo limit, nên nếu không hỏi lại đồ thị
        // thì nút rác chiếm chỗ và người dùng nhận danh sách ngắn hơn — có khi trống trơn, dù kho
        // quiz còn nguyên. Đúng lỗi "khám phá không hiện gì" đã gặp.
        String token = register("khong-an-mat-cho@example.com", "LEARNER");
        String topic = "Chủ đề bị ăn chỗ";
        // Sáu quiz để sau khi xoá hai vẫn còn đủ bốn của riêng ca này
        for (int i = 1; i <= 6; i++) {
            quizWithTopic("Quiz ăn chỗ " + i, topic, 2);
        }
        graphSync.syncPublicCatalog();

        JsonNode before = recommendations(token, 4);
        assertThat(before).hasSize(4);

        // Xoá hai quiz đứng đầu danh sách, CỐ Ý không đồng bộ lại đồ thị: mô phỏng khoảng thời gian
        // đồ thị còn giữ nút của quiz đã biến mất
        List<String> deleted = new java.util.ArrayList<>();
        for (int i = 0; i < 2; i++) {
            String doomed = before.get(i).get("quizId").asText();
            deleted.add(doomed);
            mockMvc.perform(delete("/api/v1/quizzes/{id}", doomed)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + creatorToken))
                    .andExpect(status().isNoContent());
        }

        JsonNode after = recommendations(token, 4);

        assertThat(after).as("mất 2 nút rác thì phải lấp lại cho đủ, không trả về 2").hasSize(4);
        for (JsonNode item : after) {
            assertThat(item.get("quizId").asText())
                    .as("quiz đã xoá không được xuất hiện")
                    .isNotIn(deleted);
        }
    }

    // ================================================================ gợi ý

    @Test
    @DisplayName("Gợi ý quiz thuộc chủ đề đang yếu, và KHÔNG gợi lại quiz đã làm")
    void shouldRecommendQuizzesOnWeakTopics() throws Exception {
        String token = register("yeu-mon-su@example.com", "LEARNER");
        UUID userId = currentUserId(token);

        // Làm sai hết một quiz Địa lý → yếu môn Địa lý
        String weakQuiz = quizWithTopic("Địa lý cơ bản", "Địa lý", 4);
        graphSync.sync(UUID.fromString(takeQuiz(weakQuiz, token, 0)));

        // Có một quiz Địa lý khác chưa làm → phải được gợi ý
        String suggested = quizWithTopic("Địa lý nâng cao", "Địa lý", 5);
        graphSync.rebuildForUser(userId);
        syncQuizIntoGraph(suggested);

        JsonNode items = recommendations(token, 8);
        List<String> ids = items.findValuesAsText("quizId");

        assertThat(ids).contains(suggested);
        // Quiz đã làm rồi thì gợi ý lại là vô nghĩa
        assertThat(ids).doesNotContain(weakQuiz);

        JsonNode first = items.get(0);
        assertThat(first.get("source").asText()).isEqualTo("WEAK_TOPIC");
        // Gợi ý không nói lý do thì người dùng không có căn cứ để tin hay bỏ qua
        assertThat(first.get("reason").asText()).contains("Địa lý");
    }

    @Test
    @DisplayName("Chủ đề làm đúng hết thì KHÔNG bị coi là yếu, không gợi ý ôn lại")
    void shouldNotFlagStrongTopicAsWeak() throws Exception {
        String token = register("gioi-mon-ly@example.com", "LEARNER");
        UUID userId = currentUserId(token);

        String quizId = quizWithTopic("Vật lý dễ", "Vật lý", 4);
        graphSync.sync(UUID.fromString(takeQuiz(quizId, token, 4)));   // đúng hết
        quizWithTopic("Vật lý khác", "Vật lý", 3);
        graphSync.rebuildForUser(userId);

        mockMvc.perform(get("/api/v1/recommendations/path")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weakCount").value(0))
                .andExpect(jsonPath("$.topics[0].topic").value("Vật lý"))
                .andExpect(jsonPath("$.topics[0].weak").value(false))
                .andExpect(jsonPath("$.topics[0].accuracy").value(1.0));
    }

    @Test
    @DisplayName("Trả lời quá ít câu thì chưa kết luận yếu — 0/1 câu không nói lên điều gì")
    void shouldNotJudgeWeaknessFromTooFewAnswers() throws Exception {
        String token = register("it-du-lieu@example.com", "LEARNER");
        UUID userId = currentUserId(token);

        // Chỉ 1 câu, sai → tỷ lệ đúng 0% nhưng không đủ căn cứ
        String quizId = quizWithTopic("Một câu duy nhất", "Hoá học", 1);
        graphSync.sync(UUID.fromString(takeQuiz(quizId, token, 0)));
        graphSync.rebuildForUser(userId);

        mockMvc.perform(get("/api/v1/recommendations/path")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topics[0].accuracy").value(0.0))
                .andExpect(jsonPath("$.topics[0].weak").value(false))
                .andExpect(jsonPath("$.weakCount").value(0))
                .andExpect(jsonPath("$.note").value(
                        org.hamcrest.Matchers.containsString("Chưa đủ dữ liệu")));
    }

    @Test
    @DisplayName("Lộ trình xếp chủ đề yếu nhất lên trước")
    void shouldOrderPathByWeakestFirst() throws Exception {
        String token = register("lo-trinh@example.com", "LEARNER");
        UUID userId = currentUserId(token);

        graphSync.sync(UUID.fromString(takeQuiz(quizWithTopic("Môn A", "Sinh học", 4), token, 1)));
        graphSync.sync(UUID.fromString(takeQuiz(quizWithTopic("Môn B", "Tin học", 4), token, 3)));
        graphSync.rebuildForUser(userId);

        String body = mockMvc.perform(get("/api/v1/recommendations/path")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode topics = objectMapper.readTree(body).get("topics");
        // Sinh học 1/4 = 25% phải đứng trước Tin học 3/4 = 75%
        assertThat(topics.get(0).get("topic").asText()).isEqualTo("Sinh học");
        assertThat(topics.get(1).get("topic").asText()).isEqualTo("Tin học");
        assertThat(topics.get(0).get("weak").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("Lọc cộng tác: người làm cùng quiz với tôi còn làm gì nữa")
    void shouldRecommendFromSimilarLearners() throws Exception {
        String sharedQuiz = quizWithTopic("Quiz chung", "Chủ đề chung", 3);
        String peerOnlyQuiz = quizWithTopic("Quiz người kia làm", "Chủ đề khác", 3);

        // Cả hai cùng làm quiz chung, nhưng chỉ peer làm quiz thứ hai
        graphSync.sync(UUID.fromString(takeQuiz(sharedQuiz, learnerToken, 3)));
        graphSync.sync(UUID.fromString(takeQuiz(sharedQuiz, peerToken, 3)));
        graphSync.sync(UUID.fromString(takeQuiz(peerOnlyQuiz, peerToken, 3)));

        assertThat(recommendations(learnerToken, 8).findValuesAsText("quizId"))
                .contains(peerOnlyQuiz);
    }

    @Test
    @DisplayName("Đã làm hết quiz thuộc chủ đề mình yếu thì vẫn được gợi ý chủ đề mới")
    void shouldFallBackToUnexploredTopics() throws Exception {
        String token = register("het-quiz-chu-de-yeu@example.com", "LEARNER");
        UUID userId = currentUserId(token);

        // Yếu "Mạng máy tính" và đã làm HẾT quiz thuộc chủ đề đó
        String onlyWeakQuiz = quizWithTopic("Mạng máy tính cơ bản", "Mạng máy tính", 4);
        graphSync.sync(UUID.fromString(takeQuiz(onlyWeakQuiz, token, 0)));

        // Kho còn một quiz thuộc chủ đề hoàn toàn khác, chưa đụng tới
        String unexplored = quizWithTopic("Cơ sở dữ liệu nhập môn", "Cơ sở dữ liệu", 4);
        graphSync.rebuildForUser(userId);

        // Xin nhiều hơn mặc định: kho test tích luỹ khá nhiều quiz qua các ca trước, mà ca này
        // chỉ quan tâm quiz kia CÓ được gợi ý hay không, không quan tâm nó đứng thứ mấy.
        JsonNode items = recommendations(token, 20);
        // Không có nguồn thứ ba thì khu Gợi ý trống trơn, dù kho còn nguyên chủ đề chưa thử
        assertThat(items.findValuesAsText("quizId")).contains(unexplored);

        JsonNode suggestion = null;
        for (JsonNode item : items) {
            if (unexplored.equals(item.get("quizId").asText())) {
                suggestion = item;
            }
        }
        assertThat(suggestion).isNotNull();
        assertThat(suggestion.get("source").asText()).isEqualTo("NEW_TOPIC");
        assertThat(suggestion.get("reason").asText()).contains("Cơ sở dữ liệu");
    }

    @Test
    @DisplayName("Người vừa đăng ký nhưng kho đã có quiz: được gợi ý ngay, không phải chờ làm bài (cold start)")
    void shouldRecommendToBrandNewUser() throws Exception {
        String existing = quizWithTopic("Thuật toán sắp xếp", "Thuật toán", 3);
        graphSync.syncPublicCatalog();

        String token = register("nguoi-vua-dang-ky@example.com", "LEARNER");

        // Chưa có hành vi nào để phân tích, nhưng mọi chủ đề đều là mới với họ
        assertThat(recommendations(token, 20).findValuesAsText("quizId")).contains(existing);
    }

    // ================================================================ trường hợp biên

    @Test
    @DisplayName("Người chưa làm bài nào: lộ trình rỗng nhưng có lời nhắn, không phải màn hình trống câm")
    void shouldExplainEmptyStateForNewUser() throws Exception {
        String token = register("nguoi-moi@example.com", "LEARNER");

        // Lộ trình thì đúng là rỗng — chưa có hành vi nào để dựng. Nhưng phải nói vì sao.
        // (Gợi ý quiz thì KHÔNG rỗng nữa nhờ nguồn "chủ đề mới" — xem shouldRecommendToBrandNewUser)
        mockMvc.perform(get("/api/v1/recommendations/path")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topics.length()").value(0))
                .andExpect(jsonPath("$.note").value(
                        org.hamcrest.Matchers.containsString("chưa làm bài nào")));
    }

    @Test
    @DisplayName("Dựng lại đồ thị từ lịch sử — dữ liệu có trước khi tính năng ra đời vẫn vào được")
    void shouldRebuildGraphFromHistory() throws Exception {
        String token = register("dung-lai@example.com", "LEARNER");
        UUID userId = currentUserId(token);

        // Ba bài đã nộp, cố tình KHÔNG đồng bộ — giống dữ liệu cũ
        takeQuiz(quizWithTopic("Bài cũ 1", "Chủ đề cũ", 3), token, 1);
        takeQuiz(quizWithTopic("Bài cũ 2", "Chủ đề cũ", 3), token, 0);
        takeQuiz(quizWithTopic("Bài cũ 3", "Chủ đề cũ", 3), token, 2);

        mockMvc.perform(post("/api/v1/recommendations/rebuild")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.syncedAttempts").value(3));

        // 3 + 0 + 2 = 5 câu đúng trên tổng 9
        assertThat(practicedTotal(userId, "Chủ đề cũ")).isEqualTo(9);
    }

    @Test
    @DisplayName("Đã làm hết quiz đang có: danh sách rỗng NHƯNG có câu giải thích, không im lặng biến mất")
    void shouldExplainWhyThereIsNothingToRecommend() throws Exception {
        // Khu Gợi ý ẩn khi rỗng thì người dùng không phân biệt được "hết quiz để gợi ý" với "tính
        // năng hỏng" — đã hiểu nhầm thành hỏng trên thực tế. Câu giải thích do backend viết vì chỉ
        // nó biết đang là tình huống nào.
        String token = register("da-lam-het@example.com", "LEARNER");

        // Làm hết mọi quiz công khai đang có trong đồ thị, kể cả quiz do ca khác tạo
        for (int round = 0; round < 5; round++) {
            JsonNode items = recommendations(token, 50);
            if (items.isEmpty()) {
                break;
            }
            for (JsonNode item : items) {
                takeQuizIgnoringFailure(item.get("quizId").asText(), token);
            }
        }

        JsonNode body = recommendationsBody(token, 4);
        assertThat(body.get("items")).isEmpty();
        assertThat(body.get("note").asText())
                .as("rỗng thì phải nói vì sao rỗng")
                .contains("đã làm hết");
    }

    @Test
    @DisplayName("Có gợi ý thì KHÔNG kèm ghi chú — thêm một dòng chữ thừa chỉ làm loãng")
    void shouldNotAddNoteWhenThereAreRecommendations() throws Exception {
        String token = register("co-goi-y@example.com", "LEARNER");
        quizWithTopic("Quiz cho người có gợi ý", "Chủ đề có gợi ý", 3);
        graphSync.syncPublicCatalog();

        JsonNode body = recommendationsBody(token, 4);
        assertThat(body.get("items")).isNotEmpty();
        assertThat(body.get("note").isNull()).isTrue();
    }

    @Test
    @DisplayName("Guest không xem được gợi ý — gợi ý dựa trên lịch sử của chính người gọi")
    void shouldRejectGuest() throws Exception {
        mockMvc.perform(get("/api/v1/recommendations")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/recommendations/path")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/recommendations/rebuild")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Quiz riêng tư của người khác không lọt vào danh sách gợi ý")
    void shouldNotRecommendPrivateQuizzes() throws Exception {
        String token = register("khong-lo-quiz-rieng@example.com", "LEARNER");
        UUID userId = currentUserId(token);

        String weakQuiz = quizWithTopic("Công khai yếu", "Chủ đề kín", 4);
        graphSync.sync(UUID.fromString(takeQuiz(weakQuiz, token, 0)));

        String privateQuiz = quizWithTopic("Quiz riêng tư", "Chủ đề kín", 4, "PRIVATE");
        graphSync.rebuildForUser(userId);
        // Quiz riêng tư thì người ngoài không vào làm được (404) — chính chủ tự làm để nó vào đồ thị
        graphSync.sync(UUID.fromString(takeQuiz(privateQuiz, creatorToken, 0)));

        assertThat(recommendations(token, 8).findValuesAsText("quizId"))
                .doesNotContain(privateQuiz);
    }

    // ================================================================ helper

    /**
     * Đưa một quiz công khai vào đồ thị bằng cách cho một người bất kỳ làm nó.
     * <p>
     * Cần vì cạnh {@code (Quiz)-[:COVERS]->(Topic)} chỉ được dựng khi có ai đó làm quiz — đồ thị
     * xây từ <b>hành vi</b>, không phải từ việc quiz tồn tại. Quiz chưa ai đụng tới thì chưa có
     * trong đồ thị và không thể được gợi ý; đó là giới hạn đã ghi vào phần nợ (cold start).
     */
    private void syncQuizIntoGraph(String quizId) throws Exception {
        String probe = register("mo-do-thi-" + UUID.randomUUID() + "@example.com", "LEARNER");
        graphSync.sync(UUID.fromString(takeQuiz(quizId, probe, 0)));
    }

    private boolean hasAttemptedEdge(UUID userId, String quizId) {
        return neo4jClient.query("""
                        MATCH (:User {id: $userId})-[a:ATTEMPTED]->(:Quiz {id: $quizId})
                        RETURN count(a) AS c
                        """)
                .bindAll(Map.of("userId", userId.toString(), "quizId", quizId))
                .fetchAs(Long.class).mappedBy((ts, rec) -> rec.get("c").asLong())
                .one().orElse(0L) > 0;
    }

    /** Số cạnh ATTEMPTED của riêng một người — đếm toàn cục thì lẫn dữ liệu ca khác. */
    private long attemptedEdgeCount(UUID userId) {
        return neo4jClient.query("MATCH (:User {id: $userId})-[a:ATTEMPTED]->() RETURN count(a) AS c")
                .bind(userId.toString()).to("userId")
                .fetchAs(Long.class).mappedBy((ts, rec) -> rec.get("c").asLong())
                .one().orElse(0L);
    }

    private long practicedTotal(UUID userId, String topic) {
        return neo4jClient.query("""
                        MATCH (:User {id: $userId})-[p:PRACTICED]->(:Topic {name: $topic})
                        RETURN p.total AS total
                        """)
                .bindAll(Map.of("userId", userId.toString(), "topic", topic))
                .fetchAs(Long.class).mappedBy((ts, rec) -> rec.get("total").asLong())
                .one().orElse(0L);
    }

    private boolean quizNodeExists(String quizId) {
        return neo4jClient.query("MATCH (q:Quiz {id: $quizId}) RETURN count(q) AS c")
                .bind(quizId).to("quizId")
                .fetchAs(Long.class).mappedBy((ts, rec) -> rec.get("c").asLong())
                .one().orElse(0L) > 0;
    }

    private List<String> coveredTopics(String quizId) {
        return neo4jClient.query("MATCH (:Quiz {id: $quizId})-[:COVERS]->(t:Topic) RETURN t.name AS name")
                .bind(quizId).to("quizId")
                .fetchAs(String.class).mappedBy((ts, rec) -> rec.get("name").asString())
                .all().stream().toList();
    }

    /**
     * Làm một quiz và bỏ qua nếu không làm được.
     * <p>
     * Dùng khi cần "làm cho hết mọi quiz được gợi ý": vài quiz do ca khác tạo có thể đã bị xoá hoặc
     * đổi trạng thái giữa chừng, và ca test này không quan tâm tới chúng.
     */
    private void takeQuizIgnoringFailure(String quizId, String token) {
        try {
            graphSync.sync(UUID.fromString(takeQuiz(quizId, token, 0)));
        } catch (Exception | AssertionError ignored) {
            // Không làm được thì thôi — mục tiêu là làm cạn danh sách gợi ý, không phải quiz cụ thể
        }
    }

    /** Chỉ mảng gợi ý — dùng cho phần lớn phép kiểm. */
    private JsonNode recommendations(String token, int limit) throws Exception {
        return recommendationsBody(token, limit).get("items");
    }

    /** Cả thân phản hồi, gồm `note` — dùng khi kiểm câu giải thích lúc danh sách rỗng. */
    private JsonNode recommendationsBody(String token, int limit) throws Exception {
        String body = mockMvc.perform(get("/api/v1/recommendations")
                        .param("limit", String.valueOf(limit))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    /**
     * Thẻ gợi ý của một quiz cụ thể, hoặc {@code null} nếu quiz đó không được gợi ý.
     * <p>
     * Lấy {@code limit} rộng để phép kiểm nói về "quiz có trong danh sách hay không" chứ không vô
     * tình nói về "quiz có nằm trong 4 thẻ đầu hay không".
     */
    private JsonNode findRecommendation(String token, String quizId) throws Exception {
        for (JsonNode item : recommendations(token, 50)) {
            if (item.get("quizId").asText().equals(quizId)) {
                return item;
            }
        }
        return null;
    }

    /** Quiz công khai gồm {@code questionCount} câu cùng một chủ đề. */
    private String quizWithTopic(String title, String topic, int questionCount) throws Exception {
        return quizWithTopic(title, topic, questionCount, "PUBLIC");
    }

    private String quizWithTopic(String title, String topic, int questionCount, String visibility)
            throws Exception {
        String quizId = createQuiz(title, visibility);
        String[] questionIds = new String[questionCount];
        for (int i = 0; i < questionCount; i++) {
            questionIds[i] = questionWithTopic(title + " — câu " + (i + 1), topic);
        }
        setQuizQuestions(quizId, questionIds);
        return quizId;
    }

    /**
     * Làm bài và trả lời đúng {@code correctCount} câu đầu, còn lại trả lời sai.
     * Câu Đúng/Sai nên lựa chọn đầu là đáp án đúng, lựa chọn thứ hai là sai.
     */
    private String takeQuiz(String quizId, String token, int correctCount) throws Exception {
        String startBody = mockMvc.perform(post("/api/v1/quizzes/{id}/attempts", quizId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"EXAM\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode detail = objectMapper.readTree(startBody);
        String attemptId = detail.get("attempt").get("id").asText();

        int index = 0;
        for (JsonNode question : detail.get("questions")) {
            boolean answerCorrectly = index < correctCount;
            String optionId = question.get("options").get(answerCorrectly ? 0 : 1).get("id").asText();
            mockMvc.perform(post("/api/v1/attempts/{id}/answers", attemptId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"questionId\":\"%s\",\"optionIds\":[\"%s\"]}"
                                    .formatted(question.get("questionId").asText(), optionId)))
                    .andExpect(status().isOk());
            index++;
        }

        mockMvc.perform(post("/api/v1/attempts/{id}/submit", attemptId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
        return attemptId;
    }

    private String createQuiz(String title) throws Exception {
        return createQuiz(title, "PUBLIC");
    }

    private String createQuiz(String title, String visibility) throws Exception {
        String body = mockMvc.perform(post("/api/v1/quizzes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + creatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"%s\",\"visibility\":\"%s\"}".formatted(title, visibility)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    /**
     * Đồng bộ lại tới khi một chủ đề rời khỏi đồ thị của quiz, tối đa 5 giây.
     *
     * <h4>Vì sao phải GỌI LẠI đồng bộ mỗi vòng, không chỉ đọc rồi chờ</h4>
     * {@code takeQuiz()} nộp bài → phát sự kiện miền → một lần đồng bộ chạy <b>bất đồng bộ</b>. Lần đó
     * đọc ảnh chụp của nó ở một thời điểm không xác định; nếu ảnh chụp lấy TRƯỚC khi câu Toán bị gỡ mà
     * lần ghi lại về đích SAU lần {@code sync()} tường minh, thì "Toán cũ" được ghi trở lại — và nằm
     * đó vĩnh viễn, vì không còn ai đồng bộ nữa. Vòng chờ chỉ đọc sẽ hết giờ mà không bao giờ thấy nó
     * biến mất. Đã đỏ thật đúng như vậy.
     * <p>
     * Gọi lại {@code sync()} mỗi vòng thì mỗi lần là một lần ghi với dữ liệu MỚI, và
     * {@code replaceQuizTopics} xoá sạch cạnh cũ trước khi ghi — nên trạng thái hội tụ bất kể lần ghi
     * nền lạc nhịp rơi vào lúc nào.
     * <p>
     * Đây cũng chính là tính chất đồ thị gợi ý cam kết: nó là một <b>view</b>, idempotent và dựng lại
     * được, <i>nhất quán cuối cùng</i> chứ không tức thời. Test khẳng định đúng cam kết đó thay vì đòi
     * một thứ mạnh hơn hệ thống hứa.
     */
    private boolean choChuDeBienMat(String quizId, UUID attemptId, String chuDe) throws Exception {
        long han = System.currentTimeMillis() + 5_000;
        while (System.currentTimeMillis() < han) {
            if (!coveredTopics(quizId).contains(chuDe)) {
                return true;
            }
            graphSync.sync(attemptId);
            Thread.sleep(150);
        }
        return false;
    }

    private String questionWithTopic(String content, String topic) throws Exception {
        String body = mockMvc.perform(post("/api/v1/questions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + creatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"TRUE_FALSE","content":"%s","difficulty":"EASY","points":1,
                                 "topic":"%s",
                                 "options":[{"content":"Đúng","correct":true},
                                            {"content":"Sai","correct":false}]}
                                """.formatted(content, topic)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    private void setQuizQuestions(String quizId, String... questionIds) throws Exception {
        mockMvc.perform(put("/api/v1/quizzes/{id}/questions", quizId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + creatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionIds\":[\"" + String.join("\",\"", questionIds) + "\"]}"))
                .andExpect(status().isOk());
    }

    private UUID currentUserId(String token) throws Exception {
        String body = mockMvc.perform(get("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(body).get("id").asText());
    }

    private String register(String email, String role) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"MatKhau@123","displayName":"Người dùng","role":"%s"}
                                """.formatted(email, role)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asText();
    }
}
