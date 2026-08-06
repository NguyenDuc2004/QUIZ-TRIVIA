package com.datn.quizai.attempt;

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
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test end-to-end lát cắt làm bài quiz (docs/features/03-gameplay.md) trên PostgreSQL + Redis thật.
 * <p>
 * Trọng tâm là ba tính chất khó thấy bằng mắt: chấm đúng điểm, <b>không lộ đáp án</b> khi bài chưa
 * nộp, và đề đã chốt thì chủ quiz sửa quiz cũng không ảnh hưởng bài đang làm.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
// PER_CLASS để các tài khoản fixture chỉ đăng ký một lần cho cả class
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AttemptFlowIntegrationTest {

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

    private String creatorToken;
    private String learnerToken;
    private String otherLearnerToken;

    @BeforeAll
    void registerUsers() throws Exception {
        creatorToken = register("creator-attempt@example.com", "CREATOR");
        learnerToken = register("learner-attempt@example.com", "LEARNER");
        otherLearnerToken = register("learner-khac-attempt@example.com", "LEARNER");
    }

    @Test
    @DisplayName("Luồng đầy đủ chế độ thi: bắt đầu → trả lời → nộp → chấm đúng điểm và hiện giải thích")
    void shouldRunFullExamFlow() throws Exception {
        String quizId = createQuiz("Quiz chấm điểm", "PUBLIC", 600);
        String single = createQuestion("SINGLE_CHOICE", "Thủ đô Việt Nam?", 2);
        String multiple = createQuestion("MULTIPLE_CHOICE", "Ngôn ngữ nào chạy trên JVM?", 3);
        attachQuestions(quizId, single, multiple,
                createQuestion("TRUE_FALSE", "HTTP là phi trạng thái?", 1));

        JsonNode attempt = startAttempt(quizId, "EXAM", learnerToken);
        String attemptId = attempt.get("attempt").get("id").asText();

        assertThat(attempt.get("attempt").get("maxScore").asInt()).isEqualTo(6);
        assertThat(attempt.get("questions")).hasSize(3);

        answerWithCorrectOptions(attemptId, single);
        answerWithCorrectOptions(attemptId, multiple);
        // Cố tình bỏ trống câu Đúng/Sai → phải bị tính 0 điểm

        mockMvc.perform(post("/api/v1/attempts/{id}/submit", attemptId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + learnerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attempt.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.attempt.totalScore").value(5))
                .andExpect(jsonPath("$.attempt.maxScore").value(6))
                .andExpect(jsonPath("$.attempt.correctCount").value(2))
                .andExpect(jsonPath("$.attempt.answeredCount").value(2))
                .andExpect(jsonPath("$.attempt.durationSec").isNumber())
                // Sau khi nộp mới lộ đáp án đúng và giải thích (FR-17)
                .andExpect(jsonPath("$.questions[0].correctOptionIds").isNotEmpty())
                .andExpect(jsonPath("$.questions[0].explanation").isNotEmpty())
                .andExpect(jsonPath("$.questions[2].correct").value(false))
                .andExpect(jsonPath("$.questions[2].score").value(0));
    }

    @Test
    @DisplayName("Bài chưa nộp KHÔNG trả về đáp án đúng, giải thích hay điểm")
    void shouldNotLeakAnswersWhileInProgress() throws Exception {
        String quizId = createQuiz("Quiz giấu đáp án", "PUBLIC", null);
        attachQuestions(quizId,
                createQuestion("SINGLE_CHOICE", "Câu trắc nghiệm", 1),
                createQuestion("FILL_BLANK", "Điền vào chỗ trống", 1),
                createQuestion("SHORT_ANSWER", "Trình bày ngắn", 1));

        JsonNode detail = startAttempt(quizId, "EXAM", learnerToken);

        for (JsonNode question : detail.get("questions")) {
            assertThat(question.get("correctOptionIds").isNull()).isTrue();
            assertThat(question.get("explanation").isNull()).isTrue();
            assertThat(question.get("correct").isNull()).isTrue();
            assertThat(question.get("score").isNull()).isTrue();

            // Đáp án của câu điền khuyết/tự luận nằm trong options nên phải giấu luôn options
            if (!question.get("type").asText().endsWith("_CHOICE")
                    && !question.get("type").asText().equals("TRUE_FALSE")) {
                assertThat(question.get("options")).isEmpty();
            }
        }
    }

    @Test
    @DisplayName("Chưa đăng nhập không làm được bài, không xem được bảng xếp hạng")
    void shouldBlockGuest() throws Exception {
        String quizId = createQuiz("Quiz công khai", "PUBLIC", null);
        attachQuestions(quizId, createQuestion("TRUE_FALSE", "Câu hỏi", 1));

        mockMvc.perform(post("/api/v1/quizzes/{id}/attempts", quizId))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/attempts"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/quizzes/{id}/leaderboard", quizId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Quiz riêng tư của người khác trả 404 để không tiết lộ nó tồn tại")
    void shouldHidePrivateQuiz() throws Exception {
        String quizId = createQuiz("Quiz riêng tư", "PRIVATE", null);
        attachQuestions(quizId, createQuestion("TRUE_FALSE", "Câu riêng tư", 1));

        mockMvc.perform(post("/api/v1/quizzes/{id}/attempts", quizId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + learnerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Quiz chưa có câu hỏi thì không bắt đầu được (400)")
    void shouldRejectEmptyQuiz() throws Exception {
        String quizId = createQuiz("Quiz rỗng", "PUBLIC", null);

        mockMvc.perform(post("/api/v1/quizzes/{id}/attempts", quizId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + learnerToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Gọi lại API bắt đầu khi đang có bài dở → làm tiếp bài cũ, không mất câu đã trả lời")
    void shouldResumeInProgressAttempt() throws Exception {
        String quizId = createQuiz("Quiz làm tiếp", "PUBLIC", null);
        String questionId = createQuestion("SINGLE_CHOICE", "Câu để làm dở", 1);
        attachQuestions(quizId, questionId);

        JsonNode first = startAttempt(quizId, "EXAM", learnerToken);
        String attemptId = first.get("attempt").get("id").asText();
        answerWithCorrectOptions(attemptId, questionId);

        JsonNode resumed = startAttempt(quizId, "EXAM", learnerToken);

        assertThat(resumed.get("attempt").get("id").asText()).isEqualTo(attemptId);
        assertThat(resumed.get("questions").get(0).get("userAnswer").isNull()).isFalse();
    }

    @Test
    @DisplayName("Bài làm là dữ liệu riêng: người khác truy cập trả 404")
    void shouldKeepAttemptPrivate() throws Exception {
        String quizId = createQuiz("Quiz riêng tư bài làm", "PUBLIC", null);
        attachQuestions(quizId, createQuestion("TRUE_FALSE", "Câu hỏi riêng", 1));
        String attemptId = startAttempt(quizId, "EXAM", learnerToken).get("attempt").get("id").asText();

        mockMvc.perform(get("/api/v1/attempts/{id}", attemptId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherLearnerToken))
                .andExpect(status().isNotFound());

        // Kể cả chủ quiz cũng không đọc bài làm của người học qua API này
        mockMvc.perform(get("/api/v1/attempts/{id}", attemptId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + creatorToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Chế độ luyện tập chấm ngay từng câu và không cho trả lời lại câu đã chấm")
    void shouldGradeImmediatelyInPracticeMode() throws Exception {
        String quizId = createQuiz("Quiz luyện tập", "PUBLIC", null);
        String questionId = createQuestion("SINGLE_CHOICE", "Câu luyện tập", 2);
        attachQuestions(quizId, questionId);

        JsonNode detail = startAttempt(quizId, "PRACTICE", learnerToken);
        String attemptId = detail.get("attempt").get("id").asText();
        String correctOptionId = correctOptionIdsOf(questionId).get(0);

        mockMvc.perform(post("/api/v1/attempts/{id}/answers", attemptId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + learnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"questionId":"%s","optionIds":["%s"]}
                                """.formatted(questionId, correctOptionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(true))
                .andExpect(jsonPath("$.score").value(2))
                .andExpect(jsonPath("$.explanation").isNotEmpty());

        mockMvc.perform(post("/api/v1/attempts/{id}/answers", attemptId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + learnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"questionId":"%s","optionIds":[]}
                                """.formatted(questionId)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Chế độ thi không tiết lộ đúng/sai khi vừa trả lời")
    void shouldNotRevealCorrectnessInExamMode() throws Exception {
        String quizId = createQuiz("Quiz thi kín", "PUBLIC", null);
        String questionId = createQuestion("SINGLE_CHOICE", "Câu thi", 1);
        attachQuestions(quizId, questionId);

        JsonNode detail = startAttempt(quizId, "EXAM", learnerToken);
        String attemptId = detail.get("attempt").get("id").asText();

        mockMvc.perform(post("/api/v1/attempts/{id}/answers", attemptId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + learnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"questionId":"%s","optionIds":["%s"]}
                                """.formatted(questionId, correctOptionIdsOf(questionId).get(0))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").doesNotExist())
                .andExpect(jsonPath("$.correctOptionIds").doesNotExist())
                .andExpect(jsonPath("$.answeredCount").value(1));
    }

    @Test
    @DisplayName("Chặn id lựa chọn không thuộc câu hỏi và câu hỏi ngoài đề")
    void shouldValidateAnswerPayload() throws Exception {
        String quizId = createQuiz("Quiz kiểm tra payload", "PUBLIC", null);
        String inQuiz = createQuestion("SINGLE_CHOICE", "Câu trong đề", 1);
        String outsideQuiz = createQuestion("SINGLE_CHOICE", "Câu ngoài đề", 1);
        attachQuestions(quizId, inQuiz);

        JsonNode detail = startAttempt(quizId, "EXAM", learnerToken);
        String attemptId = detail.get("attempt").get("id").asText();

        // Id lựa chọn lấy từ câu hỏi khác
        String foreignOption = optionIdsOfBank(outsideQuiz).get(0);
        mockMvc.perform(post("/api/v1/attempts/{id}/answers", attemptId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + learnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"questionId":"%s","optionIds":["%s"]}
                                """.formatted(inQuiz, foreignOption)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/attempts/{id}/answers", attemptId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + learnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"questionId":"%s","optionIds":[]}
                                """.formatted(outsideQuiz)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Nộp bài hai lần trả cùng kết quả; sau khi nộp không sửa được đáp án")
    void shouldBeIdempotentOnSubmit() throws Exception {
        String quizId = createQuiz("Quiz nộp hai lần", "PUBLIC", null);
        String questionId = createQuestion("SINGLE_CHOICE", "Câu nộp hai lần", 1);
        attachQuestions(quizId, questionId);

        JsonNode detail = startAttempt(quizId, "EXAM", learnerToken);
        String attemptId = detail.get("attempt").get("id").asText();
        answerWithCorrectOptions(attemptId, questionId);

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/v1/attempts/{id}/submit", attemptId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + learnerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.attempt.totalScore").value(1))
                    .andExpect(jsonPath("$.attempt.status").value("SUBMITTED"));
        }

        mockMvc.perform(post("/api/v1/attempts/{id}/answers", attemptId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + learnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"questionId":"%s","optionIds":[]}
                                """.formatted(questionId)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Hết giờ thì bài tự chuyển sang EXPIRED và vẫn được chấm phần đã làm (FR-16)")
    void shouldExpireWhenTimeIsUp() throws Exception {
        String quizId = createQuiz("Quiz một giây", "PUBLIC", 1);
        String questionId = createQuestion("SINGLE_CHOICE", "Câu hết giờ", 2);
        attachQuestions(quizId, questionId);

        JsonNode detail = startAttempt(quizId, "EXAM", learnerToken);
        String attemptId = detail.get("attempt").get("id").asText();
        answerWithCorrectOptions(attemptId, questionId);

        Thread.sleep(1200);

        // Hết giờ thì không nhận câu trả lời mới nữa
        mockMvc.perform(post("/api/v1/attempts/{id}/answers", attemptId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + learnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"questionId":"%s","optionIds":[]}
                                """.formatted(questionId)))
                .andExpect(status().isConflict());

        // Chỉ cần xem lại bài là hệ thống tự chốt, giữ nguyên điểm phần đã trả lời
        mockMvc.perform(get("/api/v1/attempts/{id}", attemptId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + learnerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attempt.status").value("EXPIRED"))
                .andExpect(jsonPath("$.attempt.totalScore").value(2));
    }

    @Test
    @DisplayName("Đề đã chốt: chủ quiz sửa danh sách câu hỏi giữa chừng không ảnh hưởng bài đang làm")
    void shouldFreezeQuestionsAtStart() throws Exception {
        String quizId = createQuiz("Quiz bị sửa giữa chừng", "PUBLIC", null);
        String first = createQuestion("SINGLE_CHOICE", "Câu ban đầu", 1);
        String second = createQuestion("SINGLE_CHOICE", "Câu bị gỡ", 1);
        attachQuestions(quizId, first, second);

        JsonNode detail = startAttempt(quizId, "EXAM", learnerToken);
        String attemptId = detail.get("attempt").get("id").asText();
        assertThat(detail.get("attempt").get("maxScore").asInt()).isEqualTo(2);

        // Chủ quiz gỡ bớt một câu sau khi người học đã bắt đầu
        attachQuestions(quizId, first);

        mockMvc.perform(get("/api/v1/attempts/{id}", attemptId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + learnerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions.length()").value(2))
                .andExpect(jsonPath("$.attempt.maxScore").value(2));
    }

    @Test
    @DisplayName("Lịch sử chỉ thấy bài của mình, kèm số câu đúng")
    void shouldListOwnHistoryOnly() throws Exception {
        String quizId = createQuiz("Quiz lịch sử", "PUBLIC", null);
        String questionId = createQuestion("SINGLE_CHOICE", "Câu lịch sử", 1);
        attachQuestions(quizId, questionId);

        JsonNode detail = startAttempt(quizId, "EXAM", learnerToken);
        String attemptId = detail.get("attempt").get("id").asText();
        answerWithCorrectOptions(attemptId, questionId);
        submit(attemptId, learnerToken);

        mockMvc.perform(get("/api/v1/attempts").param("quizId", quizId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + learnerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].questionCount").value(1))
                .andExpect(jsonPath("$.content[0].correctCount").value(1))
                .andExpect(jsonPath("$.content[0].answeredCount").value(1));

        mockMvc.perform(get("/api/v1/attempts").param("quizId", quizId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherLearnerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("Bảng xếp hạng: mỗi người một dòng tốt nhất, xếp theo điểm giảm dần")
    void shouldRankBestAttemptPerUser() throws Exception {
        String quizId = createQuiz("Quiz xếp hạng", "PUBLIC", null);
        String questionId = createQuestion("SINGLE_CHOICE", "Câu xếp hạng", 5);
        attachQuestions(quizId, questionId);

        // Người thứ nhất làm hai lần: lần đầu sai, lần sau đúng → bảng chỉ lấy lần đúng
        JsonNode wrong = startAttempt(quizId, "EXAM", learnerToken);
        submit(wrong.get("attempt").get("id").asText(), learnerToken);

        JsonNode right = startAttempt(quizId, "EXAM", learnerToken);
        String rightId = right.get("attempt").get("id").asText();
        answerWithCorrectOptions(rightId, questionId);
        submit(rightId, learnerToken);

        // Người thứ hai bỏ trống → 0 điểm, xếp sau
        JsonNode second = startAttempt(quizId, "EXAM", otherLearnerToken);
        submit(second.get("attempt").get("id").asText(), otherLearnerToken);

        mockMvc.perform(get("/api/v1/quizzes/{id}/leaderboard", quizId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + learnerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].rank").value(1))
                .andExpect(jsonPath("$[0].totalScore").value(5))
                .andExpect(jsonPath("$[1].rank").value(2))
                .andExpect(jsonPath("$[1].totalScore").value(0));
    }

    @Test
    @DisplayName("Bài của chính chủ quiz không lên bảng xếp hạng (biết trước đáp án)")
    void shouldExcludeQuizOwnerFromLeaderboard() throws Exception {
        String quizId = createQuiz("Quiz chủ tự làm", "PUBLIC", null);
        String questionId = createQuestion("SINGLE_CHOICE", "Câu chủ quiz tự làm", 5);
        attachQuestions(quizId, questionId);

        // Chủ quiz làm và được điểm tuyệt đối
        JsonNode ownerAttempt = startAttempt(quizId, "EXAM", creatorToken);
        String ownerAttemptId = ownerAttempt.get("attempt").get("id").asText();
        answerWithCorrectOptions(ownerAttemptId, questionId, creatorToken);
        submit(ownerAttemptId, creatorToken);

        // Bảng xếp hạng vẫn rỗng vì chưa có người học nào nộp
        mockMvc.perform(get("/api/v1/quizzes/{id}/leaderboard", quizId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + creatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // Người học nộp bài 0 điểm vẫn đứng hạng 1, không bị bài tuyệt đối của chủ quiz chen lên
        JsonNode learnerAttempt = startAttempt(quizId, "EXAM", learnerToken);
        submit(learnerAttempt.get("attempt").get("id").asText(), learnerToken);

        mockMvc.perform(get("/api/v1/quizzes/{id}/leaderboard", quizId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + learnerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].rank").value(1))
                .andExpect(jsonPath("$[0].totalScore").value(0));

        // Chủ quiz vẫn thấy điểm của mình trong lịch sử cá nhân
        mockMvc.perform(get("/api/v1/attempts").param("quizId", quizId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + creatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].totalScore").value(5));
    }

    // ===== Helper =====

    private String register(String email, String role) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"MatKhau@123","displayName":"Người dùng test","role":"%s"}
                                """.formatted(email, role)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asText();
    }

    private String createQuiz(String title, String visibility, Integer timeLimitSec) throws Exception {
        String body = mockMvc.perform(post("/api/v1/quizzes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + creatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"%s","visibility":"%s","difficulty":"EASY","timeLimitSec":%s}
                                """.formatted(title, visibility, timeLimitSec)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    /** Câu hỏi luôn có giải thích để kiểm tra được luật "chỉ lộ sau khi nộp". */
    private String createQuestion(String type, String content, int points) throws Exception {
        String options = switch (type) {
            case "MULTIPLE_CHOICE" -> """
                    [{"content":"Java","correct":true},{"content":"Kotlin","correct":true},
                     {"content":"Python","correct":false}]""";
            case "TRUE_FALSE" -> """
                    [{"content":"Đúng","correct":true},{"content":"Sai","correct":false}]""";
            case "FILL_BLANK" -> """
                    [{"content":"SQL","correct":true}]""";
            case "SHORT_ANSWER" -> """
                    [{"content":"Đáp án mẫu","correct":true}]""";
            default -> """
                    [{"content":"Đáp án đúng","correct":true},{"content":"Đáp án sai","correct":false}]""";
        };

        String body = mockMvc.perform(post("/api/v1/questions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + creatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"%s","content":"%s","difficulty":"EASY","points":%d,
                                 "explanation":"Giải thích cho câu hỏi này","options":%s}
                                """.formatted(type, content, points, options)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    private void attachQuestions(String quizId, String... questionIds) throws Exception {
        String ids = String.join("\",\"", questionIds);
        mockMvc.perform(put("/api/v1/quizzes/{id}/questions", quizId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + creatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionIds\":[\"" + ids + "\"]}"))
                .andExpect(status().isOk());
    }

    private JsonNode startAttempt(String quizId, String mode, String token) throws Exception {
        String body = mockMvc.perform(post("/api/v1/quizzes/{id}/attempts", quizId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"%s\"}".formatted(mode)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private void submit(String attemptId, String token) throws Exception {
        mockMvc.perform(post("/api/v1/attempts/{id}/submit", attemptId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    /**
     * Trả lời một câu bằng đúng bộ đáp án đúng.
     * <p>
     * Bài đang làm không trả về cờ đúng/sai, nên lấy đáp án đúng từ API dành cho chủ quiz.
     */
    private void answerWithCorrectOptions(String attemptId, String questionId) throws Exception {
        answerWithCorrectOptions(attemptId, questionId, learnerToken);
    }

    private void answerWithCorrectOptions(String attemptId, String questionId, String token)
            throws Exception {
        List<String> correctIds = correctOptionIdsOf(questionId);
        String payload = "{\"questionId\":\"%s\",\"optionIds\":[\"%s\"]}"
                .formatted(questionId, String.join("\",\"", correctIds));

        mockMvc.perform(post("/api/v1/attempts/{id}/answers", attemptId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());
    }

    private List<String> correctOptionIdsOf(String questionId) throws Exception {
        JsonNode question = fetchQuestionFromBank(questionId);
        List<String> ids = new ArrayList<>();
        for (JsonNode option : question.get("options")) {
            if (option.get("correct").asBoolean()) {
                ids.add(option.get("id").asText());
            }
        }
        return ids;
    }

    private List<String> optionIdsOfBank(String questionId) throws Exception {
        JsonNode question = fetchQuestionFromBank(questionId);
        List<String> ids = new ArrayList<>();
        question.get("options").forEach(option -> ids.add(option.get("id").asText()));
        return ids;
    }

    private JsonNode fetchQuestionFromBank(String questionId) throws Exception {
        String body = mockMvc.perform(get("/api/v1/questions/{id}", questionId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + creatorToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }
}
