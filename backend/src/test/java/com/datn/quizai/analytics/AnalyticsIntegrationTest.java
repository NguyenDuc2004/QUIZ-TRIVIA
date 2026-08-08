package com.datn.quizai.analytics;

import com.datn.quizai.ai.provider.AiCompletion;
import com.datn.quizai.ai.provider.AiOrchestrator;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test thống kê (docs/features/09 — FR-26, FR-27) trên PostgreSQL + Redis thật.
 * <p>
 * PostgreSQL thật là điều kiện bắt buộc chứ không phải cho chắc: cả lát cắt này <i>là</i> mấy câu
 * truy vấn gộp. Một câu {@code group by} sai, một phép chia khoảng lệch một đơn vị, hay
 * {@code left join} nhân đôi số dòng — không thứ nào lộ ra khi mock repository, vì lúc đó cái được
 * kiểm chỉ là "service có gọi hàm không".
 * <p>
 * Trọng tâm:
 * <ol>
 *   <li>Số liệu gộp <b>đúng</b>, kể cả trường hợp không có dữ liệu (phân biệt "0 điểm" với "chưa
 *       làm bài nào" — hai thứ hoàn toàn khác nhau).</li>
 *   <li>Thống kê quiz <b>chỉ chủ quiz xem được</b>, người khác nhận 404 chứ không phải 403.</li>
 *   <li>Đường vào việc chấm tay chạy được từ đầu đến cuối: tìm ra bài cần chấm → đọc được bài →
 *       chấm → cờ tắt. Đây là món nợ của features/06 mà lát cắt này trả.</li>
 * </ol>
 * <p>
 * {@link AiOrchestrator} được mock: ở đây quan tâm <b>trạng thái chấm</b> (đang chờ / đã chấm /
 * hỏng) chứ không phải chất lượng chấm, và gọi mô hình thật thì mỗi lần chạy ra một điểm khác nhau.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AnalyticsIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AiOrchestrator aiOrchestrator;

    private String creatorToken;
    private String otherCreatorToken;
    private String learnerToken;
    private String peerToken;

    @BeforeAll
    void registerUsers() throws Exception {
        creatorToken = register("creator-thongke@example.com", "CREATOR");
        otherCreatorToken = register("creator-khac-thongke@example.com", "CREATOR");
        learnerToken = register("learner-thongke@example.com", "LEARNER");
        peerToken = register("peer-thongke@example.com", "LEARNER");
    }

    // ================================================================ FR-26 — tiến độ người học

    @Test
    @DisplayName("Chưa làm bài nào: điểm trung bình là null, KHÔNG phải 0")
    void shouldNotReportZeroWhenNothingDone() throws Exception {
        // 0% nghĩa là làm mà sai hết. Trả 0 cho người chưa làm gì là nói sai về họ, và trên giao
        // diện thì hai tình huống đó trông y hệt nhau.
        String freshToken = register("nguoi-moi-thongke@example.com", "LEARNER");

        mockMvc.perform(get("/api/v1/analytics/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + freshToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAttempts").value(0))
                .andExpect(jsonPath("$.distinctQuizzes").value(0))
                .andExpect(jsonPath("$.averagePercent").doesNotExist())
                .andExpect(jsonPath("$.trend").isEmpty());
    }

    @Test
    @DisplayName("Làm lại một quiz nhiều lần: đếm 3 lượt nhưng chỉ 1 quiz, đường tiến bộ xếp theo thời gian")
    void shouldSeparateAttemptCountFromQuizCount() throws Exception {
        String token = register("luyen-tap-thongke@example.com", "LEARNER");
        String quizId = quizWithChoiceQuestions("Quiz luyện lại", 4);

        takeQuiz(quizId, token, 1);
        takeQuiz(quizId, token, 2);
        takeQuiz(quizId, token, 4);

        JsonNode progress = json(get("/api/v1/analytics/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));

        assertThat(progress.get("totalAttempts").asInt()).isEqualTo(3);
        assertThat(progress.get("distinctQuizzes").asInt()).isEqualTo(1);
        // (1 + 2 + 4) / 12 = 58,3%
        assertThat(progress.get("averagePercent").asDouble()).isEqualTo(58.3);

        JsonNode trend = progress.get("trend");
        assertThat(trend).hasSize(3);
        assertThat(trend.get(0).get("percent").asDouble()).isEqualTo(25.0);
        assertThat(trend.get(2).get("percent").asDouble()).isEqualTo(100.0);
        assertThat(trend.get(0).get("quizTitle").asText()).isEqualTo("Quiz luyện lại");
    }

    @Test
    @DisplayName("Tiến độ chỉ tính bài của chính mình, không lẫn bài người khác")
    void shouldScopeProgressToCaller() throws Exception {
        // Hai tài khoản RIÊNG cho ca này. Dùng fixture chung thì con số phụ thuộc những ca chạy
        // trước nó — chạy lẻ thì đạt, chạy cả bộ thì hỏng, mà cái hỏng đó không nói gì về code.
        String aToken = register("chi-cua-toi-a@example.com", "LEARNER");
        String bToken = register("chi-cua-toi-b@example.com", "LEARNER");

        String quizId = quizWithChoiceQuestions("Quiz riêng tư từng người", 2);
        takeQuiz(quizId, aToken, 2);
        takeQuiz(quizId, bToken, 0);

        JsonNode mine = json(get("/api/v1/analytics/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + aToken));
        JsonNode theirs = json(get("/api/v1/analytics/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bToken));

        assertThat(mine.get("averagePercent").asDouble()).isEqualTo(100.0);
        assertThat(theirs.get("averagePercent").asDouble()).isZero();
    }

    @Test
    @DisplayName("Khách chưa đăng nhập không xem được tiến độ")
    void shouldRejectAnonymousProgress() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/me"))
                .andExpect(status().isUnauthorized());
    }

    // ================================================================ FR-27 — thống kê quiz

    @Test
    @DisplayName("Thống kê quiz: đếm đúng lượt/người, và phân bố điểm luôn đủ 10 khoảng")
    void shouldAggregateQuizStats() throws Exception {
        String quizId = quizWithChoiceQuestions("Quiz thống kê", 10);
        takeQuiz(quizId, learnerToken, 10);   // 100% → khoảng cuối
        takeQuiz(quizId, peerToken, 5);       // 50%  → khoảng 50–60%
        takeQuiz(quizId, peerToken, 5);       // 50%  → cùng khoảng

        JsonNode stats = json(get("/api/v1/analytics/quizzes/{id}", quizId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + creatorToken));

        assertThat(stats.get("totalAttempts").asInt()).isEqualTo(3);
        assertThat(stats.get("distinctLearners").asInt()).isEqualTo(2);
        // (10 + 5 + 5) / 30
        assertThat(stats.get("averagePercent").asDouble()).isEqualTo(66.7);
        // Không bài nào hết giờ
        assertThat(stats.get("completionPercent").asDouble()).isEqualTo(100.0);

        JsonNode buckets = stats.get("scoreDistribution");
        assertThat(buckets).hasSize(10);
        assertThat(buckets.get(5).get("label").asText()).isEqualTo("50–60%");
        assertThat(buckets.get(5).get("attemptCount").asInt()).isEqualTo(2);
        // Bài đạt điểm tối đa phải rơi vào khoảng cuối, không tạo ra khoảng thứ mười một
        assertThat(buckets.get(9).get("attemptCount").asInt()).isEqualTo(1);
        assertThat(buckets.get(0).get("attemptCount").asInt()).isZero();
    }

    @Test
    @DisplayName("Câu khó: chỉ tính câu đủ 3 lượt trả lời, câu ít lượt bị loại dù sai 100%")
    void shouldIgnoreQuestionsWithTooFewAnswers() throws Exception {
        // Câu sai 1/1 lượt vẫn là 100% sai — nhưng nó nói về số người làm, không nói về độ khó.
        String rarelyAnswered = choiceQuestion("Câu ít người làm", "Hiếm");
        String quizId = createQuiz("Quiz câu ít lượt");
        attachQuestions(quizId, rarelyAnswered);
        takeQuiz(quizId, learnerToken, 0);

        JsonNode stats = json(get("/api/v1/analytics/quizzes/{id}", quizId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + creatorToken));

        assertThat(stats.get("hardestQuestions")).isEmpty();
    }

    @Test
    @DisplayName("Câu khó: xếp đúng thứ tự, câu ai cũng sai đứng trên câu ai cũng đúng")
    void shouldRankHardestQuestions() throws Exception {
        String easy = choiceQuestion("Câu dễ", "Ôn tập");
        String hard = choiceQuestion("Câu khó", "Ôn tập");
        String quizId = createQuiz("Quiz xếp hạng câu");
        // Thứ tự trong đề: câu dễ trước. takeQuiz trả lời đúng n câu ĐẦU tiên, nên đúng 1 câu
        // nghĩa là đúng "câu dễ" và sai "câu khó" — lặp lại 3 lượt cho đủ ngưỡng.
        attachQuestions(quizId, easy, hard);
        takeQuiz(quizId, learnerToken, 1);
        takeQuiz(quizId, peerToken, 1);
        takeQuiz(quizId, peerToken, 1);

        JsonNode stats = json(get("/api/v1/analytics/quizzes/{id}", quizId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + creatorToken));

        JsonNode hardest = stats.get("hardestQuestions");
        assertThat(hardest).hasSize(1);
        assertThat(hardest.get(0).get("content").asText()).isEqualTo("Câu khó");
        assertThat(hardest.get(0).get("topic").asText()).isEqualTo("Ôn tập");
        assertThat(hardest.get(0).get("answeredCount").asInt()).isEqualTo(3);
        assertThat(hardest.get(0).get("wrongPercent").asDouble()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("Quiz của người khác trả 404 chứ không phải 403 — không tiết lộ quiz đó tồn tại")
    void shouldHideOtherPeopleQuizStats() throws Exception {
        String quizId = quizWithChoiceQuestions("Quiz không phải của bạn", 2);

        mockMvc.perform(get("/api/v1/analytics/quizzes/{id}", quizId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherCreatorToken))
                .andExpect(status().isNotFound());

        // Người học cũng vậy — làm bài trên quiz không đồng nghĩa với được xem số liệu của cả lớp
        mockMvc.perform(get("/api/v1/analytics/quizzes/{id}/attempts", quizId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + learnerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Quiz chưa ai làm: trả về khung rỗng chứ không lỗi, điểm trung bình null")
    void shouldHandleQuizWithNoAttempts() throws Exception {
        String quizId = quizWithChoiceQuestions("Quiz chưa ai làm", 3);

        mockMvc.perform(get("/api/v1/analytics/quizzes/{id}", quizId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + creatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAttempts").value(0))
                .andExpect(jsonPath("$.averagePercent").doesNotExist())
                .andExpect(jsonPath("$.completionPercent").doesNotExist())
                .andExpect(jsonPath("$.scoreDistribution.length()").value(10))
                .andExpect(jsonPath("$.hardestQuestions").isEmpty());
    }

    // ================================================================ Chấm tay — nợ từ features/06

    @Test
    @DisplayName("Danh sách bài làm: một dòng cho mỗi bài, KHÔNG nhân lên theo số câu")
    void shouldNotDuplicateRowsPerAnswer() throws Exception {
        // `left join a.answers` rồi quên `group by` là chỗ sai kinh điển: bài 5 câu thành 5 dòng.
        String quizId = quizWithChoiceQuestions("Quiz nhiều câu", 5);
        takeQuiz(quizId, learnerToken, 3);
        takeQuiz(quizId, peerToken, 1);

        JsonNode attempts = json(get("/api/v1/analytics/quizzes/{id}/attempts", quizId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + creatorToken));

        assertThat(attempts).hasSize(2);
        assertThat(attempts.get(0).get("needsManualGrading").asBoolean()).isFalse();
        assertThat(attempts.get(0).get("maxScore").asInt()).isEqualTo(5);
    }

    @Test
    @DisplayName("AI chấm hỏng: bài hiện cờ cần chấm tay, chủ quiz đọc được bài rồi chấm, cờ tắt")
    void shouldSupportFullManualGradingFlow() throws Exception {
        aiFails();
        Essay essay = essayFixture("Quiz chấm tay", 10, "Đủ 3 ý: A, B, C — mỗi ý 3 điểm.");
        answerText(essay.attemptId(), essay.questionId(), "Nguyên nhân A và B.");
        submit(essay.attemptId());
        awaitGraded(essay.attemptId());

        // 1. Chủ quiz TÌM RA bài cần chấm — trước lát cắt này không có cách nào
        JsonNode attempts = json(get("/api/v1/analytics/quizzes/{id}/attempts", essay.quizId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + creatorToken));
        assertThat(attempts).hasSize(1);
        assertThat(attempts.get(0).get("needsManualGrading").asBoolean()).isTrue();
        assertThat(attempts.get(0).get("failedAiCount").asInt()).isEqualTo(1);
        assertThat(attempts.get(0).get("learnerName").asText()).isEqualTo("Người dùng");

        // 2. Chủ quiz ĐỌC ĐƯỢC bài — chấm mà không đọc được bài thì chấm bằng gì
        JsonNode view = json(get("/api/v1/attempts/{id}/grading", essay.attemptId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + creatorToken));
        JsonNode answers = view.get("answers");
        // Chỉ câu tự luận; câu trắc nghiệm máy chấm rồi, đưa vào đây là mở rộng phạm vi vô cớ
        assertThat(answers).hasSize(1);
        assertThat(answers.get(0).get("learnerAnswer").asText()).isEqualTo("Nguyên nhân A và B.");
        assertThat(answers.get(0).get("rubric").asText()).isEqualTo("Đủ 3 ý: A, B, C — mỗi ý 3 điểm.");
        assertThat(answers.get(0).get("sampleAnswer").asText()).isEqualTo("Nguyên nhân A, B, C");
        assertThat(answers.get(0).get("gradedBy").asText()).isEqualTo("AI_FAILED");
        assertThat(answers.get(0).get("needsGrading").asBoolean()).isTrue();
        assertThat(view.get("learnerName").asText()).isEqualTo("Người dùng");

        // 3. Chấm tay
        mockMvc.perform(patch("/api/v1/attempts/{a}/answers/{b}/grade",
                        essay.attemptId(), answers.get(0).get("answerId").asText())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + creatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":6,\"feedback\":\"Thiếu ý C.\"}"))
                .andExpect(status().isOk());

        // 4. Cờ tắt, điểm vào bảng thống kê
        JsonNode after = json(get("/api/v1/analytics/quizzes/{id}/attempts", essay.quizId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + creatorToken));
        assertThat(after.get(0).get("needsManualGrading").asBoolean()).isFalse();
        assertThat(after.get(0).get("score").asInt()).isEqualTo(7);   // 1 trắc nghiệm + 6 tự luận
        assertThat(after.get(0).get("maxScore").asInt()).isEqualTo(11);
    }

    @Test
    @DisplayName("Người không sở hữu quiz KHÔNG đọc được bài làm để chấm — kể cả chính người học")
    void shouldRestrictGradingView() throws Exception {
        aiFails();
        Essay essay = essayFixture("Quiz chấm tay riêng", 10, null);
        answerText(essay.attemptId(), essay.questionId(), "Bài làm riêng tư của tôi.");
        submit(essay.attemptId());
        awaitGraded(essay.attemptId());

        // Creator khác: 404 — không được biết bài này tồn tại
        mockMvc.perform(get("/api/v1/attempts/{id}/grading", essay.attemptId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherCreatorToken))
                .andExpect(status().isNotFound());

        // Chính người học cũng 404: màn hình này là của người chấm, người học đã có
        // GET /attempts/{id} với đúng thứ họ được thấy.
        mockMvc.perform(get("/api/v1/attempts/{id}/grading", essay.attemptId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + learnerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Câu chờ AI chấm KHÔNG bị tính là câu sai trong bảng câu khó")
    void shouldExcludeUngradedAnswersFromHardQuestions() throws Exception {
        // Nếu tính câu PENDING_AI/AI_FAILED là sai thì câu tự luận nào cũng thành câu khó nhất đề,
        // và Creator sẽ đi sửa một câu hỏi không có vấn đề gì.
        aiFails();
        String quizId = createQuiz("Quiz tự luận chưa chấm");
        String choice = choiceQuestion("Câu trắc nghiệm bình thường", "Hỗn hợp");
        String essay = shortAnswerQuestion(10, null);
        attachQuestions(quizId, choice, essay);

        for (String token : new String[]{learnerToken, peerToken, peerToken}) {
            String attemptId = startAttempt(quizId, token);
            answerText(attemptId, essay, "Bài làm bất kỳ.", token);
            submit(attemptId, token);
            awaitGraded(attemptId, token);
        }

        JsonNode stats = json(get("/api/v1/analytics/quizzes/{id}", quizId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + creatorToken));

        // Câu trắc nghiệm bị bỏ trống nên sai thật — nó được phép có mặt.
        // Câu tự luận AI chưa chấm được thì KHÔNG.
        for (JsonNode question : stats.get("hardestQuestions")) {
            assertThat(question.get("content").asText())
                    .as("câu tự luận chưa chấm không được coi là câu sai")
                    .isNotEqualTo("Nêu ba nguyên nhân của hiện tượng X.");
        }
    }

    // ================================================================ helper

    private record Essay(String quizId, String attemptId, String questionId) {
    }

    /** Quiz gồm một câu trắc nghiệm 2 điểm và một câu tự luận, đã bắt đầu làm và trả lời câu trắc nghiệm. */
    private Essay essayFixture(String title, int essayPoints, String rubric) throws Exception {
        String quizId = createQuiz(title);
        String choice = choiceQuestion("Thủ đô Việt Nam?", "Địa lý");
        String essay = shortAnswerQuestion(essayPoints, rubric);
        attachQuestions(quizId, choice, essay);

        String startBody = mockMvc.perform(post("/api/v1/quizzes/{id}/attempts", quizId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + learnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"EXAM\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode detail = objectMapper.readTree(startBody);
        String attemptId = detail.get("attempt").get("id").asText();
        String correctOption = detail.get("questions").get(0).get("options").get(0).get("id").asText();

        mockMvc.perform(post("/api/v1/attempts/{id}/answers", attemptId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + learnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionId\":\"%s\",\"optionIds\":[\"%s\"]}"
                                .formatted(choice, correctOption)))
                .andExpect(status().isOk());

        return new Essay(quizId, attemptId, essay);
    }

    /** Mô hình luôn hỏng → câu tự luận dừng ở {@code AI_FAILED}, đúng tình huống cần chấm tay. */
    private void aiFails() {
        willThrow(new IllegalStateException("hết hạn mức"))
                .given(aiOrchestrator).complete(any(), anyString(), any());
        willThrow(new IllegalStateException("hết hạn mức"))
                .given(aiOrchestrator).complete(any(), anyString(), any(), anyBoolean());
    }

    @SuppressWarnings("unused")
    private void aiReturns(String json) {
        AiCompletion completion = new AiCompletion("gemini", "gemini-test", json, 0, 0, 0);
        given(aiOrchestrator.complete(any(), anyString(), any())).willReturn(completion);
        given(aiOrchestrator.complete(any(), anyString(), any(), anyBoolean())).willReturn(completion);
    }

    /** Chờ luồng chấm nền dừng hẳn — hỏi lại trạng thái, không ngủ một khoảng cố định. */
    private void awaitGraded(String attemptId) throws Exception {
        awaitGraded(attemptId, learnerToken);
    }

    private void awaitGraded(String attemptId, String token) throws Exception {
        long deadline = System.currentTimeMillis() + 15_000;
        while (System.currentTimeMillis() < deadline) {
            JsonNode detail = json(get("/api/v1/attempts/{id}", attemptId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
            if (detail.get("gradingPending").asInt() == 0) {
                return;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Chấm nền không xong sau 15 giây");
    }

    /** Quiz công khai gồm {@code count} câu Đúng/Sai, mỗi câu 1 điểm. */
    private String quizWithChoiceQuestions(String title, int count) throws Exception {
        String quizId = createQuiz(title);
        String[] ids = new String[count];
        for (int i = 0; i < count; i++) {
            ids[i] = choiceQuestion(title + " — câu " + (i + 1), "Chủ đề " + title);
        }
        attachQuestions(quizId, ids);
        return quizId;
    }

    /** Làm bài và trả lời đúng {@code correctCount} câu đầu. Lựa chọn đầu luôn là đáp án đúng. */
    private void takeQuiz(String quizId, String token, int correctCount) throws Exception {
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
            String optionId = question.get("options").get(index < correctCount ? 0 : 1).get("id").asText();
            mockMvc.perform(post("/api/v1/attempts/{id}/answers", attemptId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"questionId\":\"%s\",\"optionIds\":[\"%s\"]}"
                                    .formatted(question.get("questionId").asText(), optionId)))
                    .andExpect(status().isOk());
            index++;
        }

        submit(attemptId, token);
    }

    private String startAttempt(String quizId, String token) throws Exception {
        String body = mockMvc.perform(post("/api/v1/quizzes/{id}/attempts", quizId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"EXAM\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("attempt").get("id").asText();
    }

    private void answerText(String attemptId, String questionId, String text) throws Exception {
        answerText(attemptId, questionId, text, learnerToken);
    }

    private void answerText(String attemptId, String questionId, String text, String token)
            throws Exception {
        mockMvc.perform(post("/api/v1/attempts/{id}/answers", attemptId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionId\":\"%s\",\"text\":\"%s\"}".formatted(questionId, text)))
                .andExpect(status().isOk());
    }

    private void submit(String attemptId) throws Exception {
        submit(attemptId, learnerToken);
    }

    private void submit(String attemptId, String token) throws Exception {
        mockMvc.perform(post("/api/v1/attempts/{id}/submit", attemptId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    private String createQuiz(String title) throws Exception {
        String body = mockMvc.perform(post("/api/v1/quizzes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + creatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"%s\",\"visibility\":\"PUBLIC\"}".formatted(title)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    private String choiceQuestion(String content, String topic) throws Exception {
        return createQuestion("""
                {"type":"TRUE_FALSE","content":"%s","difficulty":"EASY","points":1,"topic":"%s",
                 "options":[{"content":"Đúng","correct":true},{"content":"Sai","correct":false}]}
                """.formatted(content, topic));
    }

    private String shortAnswerQuestion(int points, String rubric) throws Exception {
        String rubricField = rubric == null ? "" : ",\"rubric\":\"%s\"".formatted(rubric);
        return createQuestion("""
                {"type":"SHORT_ANSWER","content":"Nêu ba nguyên nhân của hiện tượng X.",
                 "difficulty":"MEDIUM","points":%d%s,"topic":"Tự luận",
                 "options":[{"content":"Nguyên nhân A, B, C","correct":true}]}
                """.formatted(points, rubricField));
    }

    private String createQuestion(String json) throws Exception {
        String body = mockMvc.perform(post("/api/v1/questions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + creatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    private void attachQuestions(String quizId, String... questionIds) throws Exception {
        mockMvc.perform(put("/api/v1/quizzes/{id}/questions", quizId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + creatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionIds\":[\"" + String.join("\",\"", questionIds) + "\"]}"))
                .andExpect(status().isOk());
    }

    private JsonNode json(org.springframework.test.web.servlet.RequestBuilder request) throws Exception {
        return objectMapper.readTree(mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
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
