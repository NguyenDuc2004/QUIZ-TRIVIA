package com.datn.quizai.quiz;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test end-to-end lát cắt quản lý quiz trên PostgreSQL + Redis thật:
 * tạo quiz → soạn câu hỏi → gắn vào quiz theo thứ tự → phân quyền giữa hai Creator.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
// PER_CLASS để 3 tài khoản fixture chỉ đăng ký một lần cho cả class
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QuizManagementIntegrationTest {

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
    private String otherCreatorToken;
    private String learnerToken;

    @BeforeAll
    void registerUsers() throws Exception {
        creatorToken = register("creator-quiz@example.com", "CREATOR");
        otherCreatorToken = register("creator-khac@example.com", "CREATOR");
        learnerToken = register("learner-quiz@example.com", "LEARNER");
    }

    @Test
    @DisplayName("Luồng đầy đủ: tạo quiz → tạo câu hỏi → gắn vào quiz theo thứ tự → đổi thứ tự")
    void shouldComposeQuizFromQuestionBank() throws Exception {
        String quizId = createQuiz(creatorToken, "Ôn tập Java", "PRIVATE");

        String firstQuestion = createQuestion(creatorToken, "SINGLE_CHOICE", "JVM là gì?");
        String secondQuestion = createQuestion(creatorToken, "TRUE_FALSE", "Java có garbage collector?");

        // Gắn 2 câu theo thứ tự
        mockMvc.perform(put("/api/v1/quizzes/{id}/questions", quizId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + creatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionIds\":[\"" + firstQuestion + "\",\"" + secondQuestion + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions.length()").value(2))
                .andExpect(jsonPath("$.questions[0].id").value(firstQuestion))
                .andExpect(jsonPath("$.questions[1].id").value(secondQuestion))
                .andExpect(jsonPath("$.quiz.questionCount").value(2));

        // Đảo thứ tự → thứ tự trả về phải đổi theo
        mockMvc.perform(put("/api/v1/quizzes/{id}/questions", quizId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + creatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionIds\":[\"" + secondQuestion + "\",\"" + firstQuestion + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions[0].id").value(secondQuestion))
                .andExpect(jsonPath("$.questions[1].id").value(firstQuestion));

        // Câu hỏi đang nằm trong quiz → không cho xóa khỏi ngân hàng
        mockMvc.perform(delete("/api/v1/questions/{id}", firstQuestion)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + creatorToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("đang được dùng")));
    }

    @Test
    @DisplayName("Quiz PRIVATE: người khác GET → 404 (không tiết lộ tài nguyên tồn tại)")
    void shouldHidePrivateQuizFromOthers() throws Exception {
        String quizId = createQuiz(creatorToken, "Quiz riêng tư", "PRIVATE");

        mockMvc.perform(get("/api/v1/quizzes/{id}", quizId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherCreatorToken))
                .andExpect(status().isNotFound());

        // Guest cũng vậy
        mockMvc.perform(get("/api/v1/quizzes/{id}", quizId))
                .andExpect(status().isNotFound());

        // Chủ sở hữu thì xem được
        mockMvc.perform(get("/api/v1/quizzes/{id}", quizId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + creatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Quiz riêng tư"));
    }

    @Test
    @DisplayName("Quiz PUBLIC: Guest xem được phần giới thiệu nhưng KHÔNG lấy được câu hỏi")
    void shouldExposePublicQuizWithoutQuestionsToGuest() throws Exception {
        String quizId = createQuiz(creatorToken, "Quiz công khai", "PUBLIC");

        mockMvc.perform(get("/api/v1/quizzes/{id}", quizId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Quiz công khai"))
                .andExpect(jsonPath("$.questions").doesNotExist());

        mockMvc.perform(get("/api/v1/quizzes/{id}/questions", quizId))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/quizzes").param("q", "công khai"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Quiz công khai"));
    }

    @Test
    @DisplayName("Creator khác sửa/xóa quiz không phải của mình → 403")
    void shouldReject403OnForeignQuiz() throws Exception {
        String quizId = createQuiz(creatorToken, "Quiz của tôi", "PUBLIC");

        mockMvc.perform(put("/api/v1/quizzes/{id}", quizId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherCreatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Chiếm quyền\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/quizzes/{id}", quizId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherCreatorToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Learner không được tạo quiz hay vào ngân hàng câu hỏi → 403")
    void shouldReject403ForLearner() throws Exception {
        mockMvc.perform(post("/api/v1/quizzes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + learnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Learner thử tạo\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/questions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + learnerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Guest gọi ?mine=true → 401; danh mục thì xem được")
    void shouldRejectGuestOnMineFilterButAllowCategories() throws Exception {
        mockMvc.perform(get("/api/v1/quizzes").param("mine", "true"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                // 6 danh mục nạp sẵn trong migration V2
                .andExpect(jsonPath("$.length()").value(6));
    }

    @Test
    @DisplayName("Gắn câu hỏi của người khác vào quiz mình → 403")
    void shouldRejectAttachingForeignQuestion() throws Exception {
        String quizId = createQuiz(creatorToken, "Quiz lắp câu lạ", "PRIVATE");
        String foreignQuestion = createQuestion(otherCreatorToken, "TRUE_FALSE", "Câu của người khác");

        mockMvc.perform(put("/api/v1/quizzes/{id}/questions", quizId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + creatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionIds\":[\"" + foreignQuestion + "\"]}"))
                .andExpect(status().isForbidden());
    }

    /**
     * Ca này từng làm hỏng thật: JPQL gọi {@code lower(:param)} với tham số null khiến
     * PostgreSQL báo {@code function lower(bytea) does not exist}. Phải test cả trường hợp
     * KHÔNG truyền bộ lọc nào, không chỉ trường hợp có từ khóa.
     */
    @Test
    @DisplayName("Liệt kê không truyền bộ lọc nào → 200 (cả quiz và ngân hàng câu hỏi)")
    void shouldListWithoutAnyFilter() throws Exception {
        createQuiz(creatorToken, "Quiz không lọc", "PUBLIC");
        createQuestion(creatorToken, "TRUE_FALSE", "Câu hỏi không lọc");

        mockMvc.perform(get("/api/v1/quizzes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        mockMvc.perform(get("/api/v1/quizzes").param("mine", "true")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + creatorToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/questions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + creatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("Lọc từ khóa không phân biệt hoa/thường và có phân trang")
    void shouldFilterByKeywordCaseInsensitively() throws Exception {
        createQuiz(creatorToken, "Đại số tuyến tính", "PUBLIC");

        mockMvc.perform(get("/api/v1/quizzes").param("q", "ĐẠI SỐ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Đại số tuyến tính"));

        mockMvc.perform(get("/api/v1/quizzes").param("q", "không-có-quiz-nào-tên-này"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/api/v1/questions").param("size", "1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + creatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    @DisplayName("Tiêu đề rỗng → 400 kèm fieldErrors")
    void shouldValidateQuizTitle() throws Exception {
        mockMvc.perform(post("/api/v1/quizzes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + creatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").value("Tiêu đề không được để trống"));
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

    private String createQuiz(String token, String title, String visibility) throws Exception {
        String body = mockMvc.perform(post("/api/v1/quizzes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"%s","visibility":"%s","difficulty":"EASY"}
                                """.formatted(title, visibility)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode created = objectMapper.readTree(body);
        assertThat(created.get("questionCount").asInt()).isZero();
        return created.get("id").asText();
    }

    private String createQuestion(String token, String type, String content) throws Exception {
        String options = switch (type) {
            case "TRUE_FALSE" -> "[{\"content\":\"Đúng\",\"correct\":true},{\"content\":\"Sai\",\"correct\":false}]";
            default -> "[{\"content\":\"A\",\"correct\":true},{\"content\":\"B\",\"correct\":false}]";
        };

        String body = mockMvc.perform(post("/api/v1/questions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"%s","content":"%s","difficulty":"EASY","points":1,"options":%s}
                                """.formatted(type, content, options)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }
}
