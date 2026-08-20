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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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

    // ============================================================ FR-12 — xuất / nhập quiz

    @Test
    @DisplayName("Xuất rồi nhập lại: nội dung đề giữ nguyên từng câu, từng lựa chọn, từng đáp án đúng")
    void shouldRoundTripQuiz() throws Exception {
        String quizId = createQuiz(creatorToken, "Ôn tập Giải tích", "PUBLIC");
        String c1 = createQuestion(creatorToken, "SINGLE_CHOICE", "Đạo hàm của x² là gì?");
        String c2 = createQuestion(creatorToken, "TRUE_FALSE", "Hàm hằng có đạo hàm bằng 0?");
        mockMvc.perform(put("/api/v1/quizzes/{id}/questions", quizId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + creatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionIds\":[\"%s\",\"%s\"]}".formatted(c1, c2)))
                .andExpect(status().isOk());

        String file = mockMvc.perform(get("/api/v1/quizzes/{id}/export", quizId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + creatorToken))
                .andExpect(status().isOk())
                // Tên tệp qua RFC 5987 để giữ dấu tiếng Việt
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.containsString("filename*=UTF-8''")))
                .andReturn().getResponse().getContentAsString();

        JsonNode xuat = objectMapper.readTree(file);
        assertThat(xuat.get("title").asText()).isEqualTo("Ôn tập Giải tích");
        assertThat(xuat.get("questions")).hasSize(2);

        // KHÔNG mang theo id / chủ sở hữu / thống kê: file là NỘI DUNG ĐỀ, không phải bản sao dòng CSDL
        assertThat(xuat.has("id")).isFalse();
        assertThat(xuat.has("ownerId")).isFalse();
        assertThat(xuat.has("attemptCount")).isFalse();
        assertThat(xuat.get("questions").get(0).has("id")).isFalse();

        // Nhập lại bằng chính file vừa xuất
        String moi = mockMvc.perform(post("/api/v1/quizzes/import")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + creatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Ôn tập Giải tích"))
                .andExpect(jsonPath("$.questionCount").value(2))
                // LUÔN riêng tư dù file ghi PUBLIC — nhập xong mà đề tự xuất hiện ở mục Khám phá là một
                // bất ngờ không dễ chịu; muốn công khai thì bấm thêm một nút
                .andExpect(jsonPath("$.visibility").value("PRIVATE"))
                .andReturn().getResponse().getContentAsString();

        String quizMoiId = objectMapper.readTree(moi).get("id").asText();
        assertThat(quizMoiId).as("nhập LUÔN tạo mới, không ghi đè quiz cũ").isNotEqualTo(quizId);

        // Xuất bản mới ra và so với bản gốc: nội dung đề phải trùng khớp
        String file2 = mockMvc.perform(get("/api/v1/quizzes/{id}/export", quizMoiId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + creatorToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode lai = objectMapper.readTree(file2);
        assertThat(lai.get("questions")).hasSize(2);
        for (int i = 0; i < 2; i++) {
            JsonNode goc = xuat.get("questions").get(i);
            JsonNode sau = lai.get("questions").get(i);
            assertThat(sau.get("content").asText()).isEqualTo(goc.get("content").asText());
            assertThat(sau.get("type").asText()).isEqualTo(goc.get("type").asText());
            // Đáp án đúng là thứ mất đi thì đề thành vô dụng mà nhìn vẫn "đủ câu"
            assertThat(sau.get("options")).isEqualTo(goc.get("options"));
        }
    }

    @Test
    @DisplayName("Quiz của người khác: không xuất được")
    void shouldNotExportOthersQuiz() throws Exception {
        String quizId = createQuiz(creatorToken, "Quiz riêng của tôi", "PRIVATE");

        mockMvc.perform(get("/api/v1/quizzes/{id}/export", quizId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherCreatorToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("File không có câu hỏi nào: từ chối 400")
    void shouldRejectEmptyImport() throws Exception {
        mockMvc.perform(post("/api/v1/quizzes/import")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + creatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"formatVersion\":1,\"title\":\"Quiz rỗng\",\"questions\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("File của phiên bản mới hơn: từ chối rõ ràng thay vì đọc bừa")
    void shouldRejectNewerFormatVersion() throws Exception {
        // Đọc bừa thì file mới có thể chứa trường bản này không hiểu, và nó IM LẶNG làm mất đúng
        // những trường đó — người dùng tưởng nhập thành công.
        mockMvc.perform(post("/api/v1/quizzes/import")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + creatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"formatVersion":99,"title":"Quiz tương lai","questions":[
                                  {"type":"TRUE_FALSE","content":"Câu hỏi",
                                   "options":[{"content":"Đúng","correct":true},
                                              {"content":"Sai","correct":false}]}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("99")));
    }

    @Test
    @DisplayName("Quiz nhập vào thuộc về NGƯỜI NHẬP, không phải người xuất")
    void importedQuizBelongsToImporter() throws Exception {
        String quizId = createQuiz(creatorToken, "Đề chia sẻ", "PUBLIC");
        String cau = createQuestion(creatorToken, "TRUE_FALSE", "Câu hỏi mẫu");
        mockMvc.perform(put("/api/v1/quizzes/{id}/questions", quizId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + creatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionIds\":[\"%s\"]}".formatted(cau)))
                .andExpect(status().isOk());

        String file = mockMvc.perform(get("/api/v1/quizzes/{id}/export", quizId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + creatorToken))
                .andReturn().getResponse().getContentAsString();

        // Người KHÁC nhập file đó — đây chính là cách dùng "chia sẻ đề cho đồng nghiệp"
        mockMvc.perform(post("/api/v1/quizzes/import")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherCreatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerId").value(idOf(otherCreatorToken)));
    }

    /** Id người dùng từ token — cần để khẳng định quiz nhập vào thuộc về ĐÚNG người nhập. */
    private String idOf(String token) throws Exception {
        String body = mockMvc.perform(get("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    // ============================================================ số NGƯỜI đã làm quiz

    @Test
    @DisplayName("Đếm NGƯỜI, không đếm LƯỢT: một người làm ba lần vẫn là một người")
    void shouldCountDistinctLearnersNotAttempts() throws Exception {
        String quizId = quizSanSang("Quiz đếm người");

        // Cùng một người làm xong ba lần
        for (int i = 0; i < 3; i++) {
            nopMotLuot(quizId, learnerToken);
        }

        // Đếm lượt thì ra 3 — và quiz trông như có ba người quan tâm trong khi chỉ có một.
        // Con số đó vừa sai vừa dễ thổi phồng, nên phải là count(distinct user_id).
        assertThat(soNguoiDaLam(quizId)).isEqualTo(1);

        // Người thứ hai vào làm
        nopMotLuot(quizId, otherLearnerToken());
        assertThat(soNguoiDaLam(quizId)).isEqualTo(2);
    }

    @Test
    @DisplayName("Bài ĐANG LÀM DỞ không tính — bấm vào rồi thoát không phải là 'đã làm'")
    void shouldNotCountUnfinishedAttempts() throws Exception {
        String quizId = quizSanSang("Quiz bỏ dở");

        // Bắt đầu nhưng KHÔNG nộp
        mockMvc.perform(post("/api/v1/quizzes/{id}/attempts", quizId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + learnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"EXAM\"}"))
                .andExpect(status().isCreated());

        assertThat(soNguoiDaLam(quizId)).isZero();
    }

    @Test
    @DisplayName("Quiz chưa ai làm trả về 0 — giao diện tự ẩn, KHÔNG hiện '0 người đã làm'")
    void shouldReturnZeroForBrandNewQuiz() throws Exception {
        String quizId = quizSanSang("Quiz mới tinh");

        // 0 ở API là đúng; nghĩa "chưa ai kịp làm" chứ không phải "quiz dở". Việc ẩn con số là của giao
        // diện — backend không được tự bịa ra một giá trị khác để né chuyện đó.
        assertThat(soNguoiDaLam(quizId)).isZero();
    }

    /** Quiz công khai đã gắn một câu hỏi, sẵn sàng cho người khác làm. */
    private String quizSanSang(String title) throws Exception {
        String quizId = createQuiz(creatorToken, title, "PUBLIC");
        String cau = createQuestion(creatorToken, "TRUE_FALSE", "Câu hỏi mẫu");
        mockMvc.perform(put("/api/v1/quizzes/{id}/questions", quizId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + creatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionIds\":[\"%s\"]}".formatted(cau)))
                .andExpect(status().isOk());
        return quizId;
    }

    /** Bắt đầu rồi nộp luôn một lượt — không trả lời câu nào, vì phép kiểm chỉ quan tâm bài đã XONG. */
    private void nopMotLuot(String quizId, String token) throws Exception {
        String body = mockMvc.perform(post("/api/v1/quizzes/{id}/attempts", quizId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"EXAM\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String attemptId = objectMapper.readTree(body).get("attempt").get("id").asText();
        mockMvc.perform(post("/api/v1/attempts/{id}/submit", attemptId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    private int soNguoiDaLam(String quizId) throws Exception {
        String body = mockMvc.perform(get("/api/v1/quizzes/{id}", quizId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + creatorToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("learnerCount").asInt();
    }

    /** Một người học thứ hai, tạo theo yêu cầu để mỗi phép kiểm tự đủ. */
    private String otherLearnerToken() throws Exception {
        return register("hocvien-" + java.util.UUID.randomUUID() + "@example.com", "LEARNER");
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

    // ================================================================ chủ đề câu hỏi

    @Test
    @DisplayName("Liệt kê chủ đề của tôi kèm số câu, xếp theo bảng chữ cái không phân biệt hoa/thường")
    void shouldListMyTopicsWithCounts() throws Exception {
        String token = register("chu-de@example.com", "CREATOR");
        createQuestionWithTopic(token, "Trận Bạch Đằng năm nào?", "Lịch sử Việt Nam");
        createQuestionWithTopic(token, "Ai ban hành bộ luật Hồng Đức?", "Lịch sử Việt Nam");
        createQuestionWithTopic(token, "Chiến tranh thế giới thứ hai kết thúc năm nào?", "Lịch sử thế giới");
        createQuestionWithTopic(token, "1 + 1 = ?", null);   // không đặt chủ đề

        mockMvc.perform(get("/api/v1/questions/topics")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                // Câu không có chủ đề không tạo ra một mục rỗng trong danh sách chọn
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].topic").value("Lịch sử thế giới"))
                .andExpect(jsonPath("$[0].questionCount").value(1))
                .andExpect(jsonPath("$[1].topic").value("Lịch sử Việt Nam"))
                // Số câu là thứ giúp người dùng biết chủ đề nào đủ câu để dựng một quiz
                .andExpect(jsonPath("$[1].questionCount").value(2));
    }

    @Test
    @DisplayName("Chủ đề của người khác không lọt vào danh sách của mình")
    void shouldNotLeakTopicsBetweenAccounts() throws Exception {
        String mine = register("chu-de-cua-toi@example.com", "CREATOR");
        String theirs = register("chu-de-nguoi-khac@example.com", "CREATOR");

        createQuestionWithTopic(theirs, "Câu của người khác", "Chủ đề riêng tư");
        createQuestionWithTopic(mine, "Câu của tôi", "Chủ đề của tôi");

        mockMvc.perform(get("/api/v1/questions/topics")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + mine))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].topic").value("Chủ đề của tôi"));
    }

    @Test
    @DisplayName("Lọc ngân hàng theo chủ đề — đây là thứ giúp dựng quiz theo môn mà không lật hết ngân hàng")
    void shouldFilterQuestionBankByTopic() throws Exception {
        String token = register("loc-chu-de@example.com", "CREATOR");
        createQuestionWithTopic(token, "Trận Bạch Đằng", "Lịch sử");
        createQuestionWithTopic(token, "Bộ luật Hồng Đức", "Lịch sử");
        createQuestionWithTopic(token, "Đạo hàm của x bình phương", "Toán");

        mockMvc.perform(get("/api/v1/questions").param("topic", "Lịch sử")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        // Lọc không phân biệt hoa/thường: người dùng gõ lại tên chủ đề khác kiểu vẫn ra
        mockMvc.perform(get("/api/v1/questions").param("topic", "lỊcH sỬ")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(get("/api/v1/questions").param("topic", "Chủ đề không tồn tại")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("Guest không xem được danh sách chủ đề — đó là dữ liệu riêng")
    void shouldRejectGuestOnTopics() throws Exception {
        mockMvc.perform(get("/api/v1/questions/topics"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Đường dẫn /topics không bị nuốt bởi /{id}")
    void shouldNotConfuseTopicsPathWithQuestionId() throws Exception {
        String token = register("duong-dan-topics@example.com", "CREATOR");

        // `/questions/{id}` nhận UUID; nếu Spring khớp nhầm thì sẽ trả 400 vì "topics" không phải UUID
        mockMvc.perform(get("/api/v1/questions/topics")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    /** Tạo câu hỏi kèm chủ đề; truyền null để tạo câu không đặt chủ đề. */
    private void createQuestionWithTopic(String token, String content, String topic) throws Exception {
        String topicField = topic == null ? "" : ",\"topic\":\"%s\"".formatted(topic);
        mockMvc.perform(post("/api/v1/questions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"TRUE_FALSE","content":"%s","difficulty":"EASY","points":1%s,
                                 "options":[{"content":"Đúng","correct":true},{"content":"Sai","correct":false}]}
                                """.formatted(content, topicField)))
                .andExpect(status().isCreated());
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
