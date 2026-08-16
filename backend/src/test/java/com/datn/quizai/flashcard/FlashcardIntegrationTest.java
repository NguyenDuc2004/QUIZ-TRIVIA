package com.datn.quizai.flashcard;

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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Flashcard và ôn tập ngắt quãng (features/11).
 * <p>
 * Thuật toán SM-2 đã có unit test riêng ({@code Sm2SchedulerTest}) nên ở đây <b>không kiểm lại phép tính
 * khoảng ôn</b>. Lớp này kiểm những thứ chỉ hỏng khi có cơ sở dữ liệu và HTTP thật:
 * <ol>
 *   <li><b>Cách ly giữa người dùng</b> — bộ thẻ của người khác phải như không tồn tại, và trạng thái ôn
 *       của hai người trên cùng một thẻ không được đè lên nhau.</li>
 *   <li><b>Thẻ mới vào được phiên ôn</b> — thẻ nằm trong danh sách mà không có trạng thái ôn thì nó vô hình
 *       với phiên ôn, và người dùng không hiểu vì sao thẻ vừa thêm không hiện ra.</li>
 *   <li><b>Thẻ quá hạn không bị bỏ rơi</b> — nghỉ vài ngày rồi quay lại vẫn phải thấy thẻ của những ngày đó.</li>
 *   <li><b>Sinh thẻ từ câu sai không tạo trùng</b> khi bấm nhiều lần.</li>
 * </ol>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FlashcardIntegrationTest {

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
    @Autowired
    private JdbcTemplate jdbc;

    private String tokenA;
    private String tokenB;

    @BeforeAll
    void setUp() throws Exception {
        tokenA = register("the-a@example.com");
        tokenB = register("the-b@example.com");
    }

    // ======================================================== 1. Cách ly giữa người dùng

    @Test
    @DisplayName("Bộ thẻ của người khác trả 404, KHÔNG phải 403")
    void otherUsersDeckLooksLikeItDoesNotExist() throws Exception {
        String deckId = createDeck(tokenA, "Bộ riêng của A");

        // 403 là xác nhận bộ thẻ đó có thật — tiết lộ thông tin cho người không có quyền biết
        mockMvc.perform(get("/api/v1/decks/{id}/cards", deckId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/v1/decks/{id}", deckId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        // Và bộ thẻ vẫn còn nguyên sau khi B thử xoá
        assertThat(jdbc.queryForObject("select count(*) from flashcard_decks where id = ?::uuid",
                Long.class, deckId)).isEqualTo(1);
    }

    @Test
    @DisplayName("Danh sách bộ thẻ chỉ trả về bộ của chính mình")
    void deckListIsScopedToOwner() throws Exception {
        createDeck(tokenA, "Chỉ A thấy bộ này");

        String body = mockMvc.perform(get("/api/v1/decks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB)
                        .param("keyword", "Chỉ A thấy"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(body).get("content")).isEmpty();
    }

    @Test
    @DisplayName("Khách chưa đăng nhập nhận 401 ở mọi endpoint flashcard")
    void guestIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/decks")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/flashcards/due")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/flashcards/stats")).andExpect(status().isUnauthorized());
    }

    // ======================================================== 2. Thẻ mới vào được phiên ôn

    @Test
    @DisplayName("Thẻ vừa thêm đến hạn NGAY và hiện trong phiên ôn")
    void newCardIsImmediatelyDue() throws Exception {
        String deckId = createDeck(tokenA, "Bộ kiểm thẻ mới");
        String cardId = addCard(tokenA, deckId, "Thủ đô Việt Nam?", "Hà Nội");

        // Thiếu bước tạo trạng thái ôn lúc thêm thẻ thì thẻ chỉ nằm trong danh sách mà không bao giờ vào
        // phiên ôn, vì phiên ôn đọc từ bảng flashcard_reviews chứ không từ bảng flashcards
        mockMvc.perform(get("/api/v1/flashcards/due")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .param("deckId", deckId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(cardId))
                .andExpect(jsonPath("$[0].dueDate").value(LocalDate.now().toString()));
    }

    @Test
    @DisplayName("Ôn xong thì thẻ rời khỏi danh sách đến hạn, và lịch mới được trả về")
    void reviewingRemovesCardFromDueList() throws Exception {
        String deckId = createDeck(tokenA, "Bộ kiểm ôn");
        String cardId = addCard(tokenA, deckId, "2 + 2 = ?", "4");

        mockMvc.perform(post("/api/v1/flashcards/{id}/review", cardId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .param("quality", "GOOD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intervalDays").value(1))
                .andExpect(jsonPath("$.dueDate").value(LocalDate.now().plusDays(1).toString()));

        mockMvc.perform(get("/api/v1/flashcards/due")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .param("deckId", deckId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("Trạng thái ôn của hai người trên CÙNG một thẻ không đè lên nhau")
    void reviewStateIsPerUser() throws Exception {
        String deckId = createDeck(tokenA, "Bộ của A để kiểm cách ly");
        String cardId = addCard(tokenA, deckId, "Nước sôi ở bao nhiêu độ?", "100°C");

        // B được cấp trạng thái ôn trên đúng thẻ đó qua cơ sở dữ liệu — mô phỏng tình huống về sau khi có
        // chia sẻ bộ thẻ. Đây là lý do trạng thái ôn phải là bảng riêng, không phải cột trên flashcards.
        String idB = jdbc.queryForObject("select id::text from users where email = ?",
                String.class, "the-b@example.com");
        jdbc.update("""
                insert into flashcard_reviews (id, flashcard_id, user_id, due_date)
                values (gen_random_uuid(), ?::uuid, ?::uuid, current_date)
                """, cardId, idB);

        mockMvc.perform(post("/api/v1/flashcards/{id}/review", cardId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .param("quality", "EASY"))
                .andExpect(status().isOk());

        // Lịch của B phải còn nguyên ở hôm nay
        assertThat(jdbc.queryForObject("""
                select due_date from flashcard_reviews where flashcard_id = ?::uuid and user_id = ?::uuid
                """, LocalDate.class, cardId, idB)).isEqualTo(LocalDate.now());
    }

    // ======================================================== 3. Thẻ quá hạn không bị bỏ rơi

    @Test
    @DisplayName("Thẻ quá hạn từ những ngày trước vẫn hiện ra — nghỉ vài ngày không làm mất thẻ")
    void overdueCardsStillAppear() throws Exception {
        String deckId = createDeck(tokenA, "Bộ kiểm quá hạn");
        String cardId = addCard(tokenA, deckId, "Câu quá hạn", "Đáp án");

        jdbc.update("""
                update flashcard_reviews set due_date = current_date - 5
                where flashcard_id = ?::uuid
                """, cardId);

        // Lọc bằng `due_date = hôm nay` thì thẻ này biến mất, và đó đúng là lúc người học cần ôn nhất
        mockMvc.perform(get("/api/v1/flashcards/due")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .param("deckId", deckId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(cardId));
    }

    // ======================================================== 4. Sinh thẻ từ câu trả lời sai

    @Test
    @DisplayName("Bấm sinh thẻ lần hai KHÔNG tạo thẻ trùng, và báo rõ số câu đã bỏ qua")
    void generatingTwiceSkipsDuplicates() throws Exception {
        // Tài khoản riêng cho ca này: lớp test dùng PER_CLASS và không dọn cơ sở dữ liệu giữa các ca, nên
        // dùng chung tài khoản thì số câu sai tích lại từ ca khác và con số mong đợi không còn xác định.
        String token = register("the-sinh-tu-cau-sai@example.com");
        String deckId = createDeck(token, "Bộ từ câu sai");
        taoMotCauTraLoiSai("the-sinh-tu-cau-sai@example.com");

        String lan1 = mockMvc.perform(post("/api/v1/decks/{id}/cards/from-wrong-answers", deckId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(lan1).get("soDaTao").asInt())
                .as("phải sinh được thẻ từ câu vừa trả lời sai").isEqualTo(1);

        String lan2 = mockMvc.perform(post("/api/v1/decks/{id}/cards/from-wrong-answers", deckId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(lan2).get("soDaTao").asInt()).isZero();
        assertThat(objectMapper.readTree(lan2).get("soBoQua").asInt()).isEqualTo(1);
    }

    @Test
    @DisplayName("Thẻ sinh từ câu sai có nguồn FROM_WRONG_ANSWER và mặt sau chứa đáp án đúng")
    void generatedCardCarriesCorrectAnswer() throws Exception {
        String token = register("the-noi-dung-the-sinh@example.com");
        String deckId = createDeck(token, "Bộ kiểm nội dung thẻ sinh");
        taoMotCauTraLoiSai("the-noi-dung-the-sinh@example.com");

        mockMvc.perform(post("/api/v1/decks/{id}/cards/from-wrong-answers", deckId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());

        String body = mockMvc.perform(get("/api/v1/decks/{id}/cards", deckId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var the = objectMapper.readTree(body).get(0);
        assertThat(the.get("source").asText()).isEqualTo("FROM_WRONG_ANSWER");
        assertThat(the.get("back").asText())
                .as("mặt sau phải có đáp án đúng, nếu không thẻ vô dụng").contains("Đáp án đúng");
        // Thẻ sinh ra cũng phải đến hạn ngay, y như thẻ tự thêm
        assertThat(the.get("dueDate").asText()).isEqualTo(LocalDate.now().toString());
    }

    // ======================================================== 5. Thống kê

    @Test
    @DisplayName("Dự báo khối lượng ôn có đúng 7 điểm, ngày không có thẻ vẫn là điểm giá trị 0")
    void forecastHasOnePointPerDay() throws Exception {
        String deckId = createDeck(tokenA, "Bộ kiểm thống kê");
        addCard(tokenA, deckId, "Câu thống kê", "Đáp án");

        String body = mockMvc.perform(get("/api/v1/flashcards/stats")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.soDenHanHomNay").exists())
                .andReturn().getResponse().getContentAsString();

        // Thiếu bù ngày trống thì biểu đồ nhảy qua khoảng trống và trông như khối lượng ôn liên tục
        assertThat(objectMapper.readTree(body).get("duBao")).hasSize(7);
    }

    // ================================================================ helper

    private String register(String email) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"MatKhau@123","displayName":"Người học","role":"LEARNER"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asText();
    }

    private String createDeck(String token, String title) throws Exception {
        String body = mockMvc.perform(post("/api/v1/decks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"%s","topic":"Kiểm thử"}
                                """.formatted(title)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    private String addCard(String token, String deckId, String front, String back) throws Exception {
        String body = mockMvc.perform(post("/api/v1/decks/{id}/cards", deckId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"front":"%s","back":"%s"}
                                """.formatted(front, back)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    /**
     * Dựng thẳng vào cơ sở dữ liệu một câu hỏi trắc nghiệm và một lượt trả lời sai.
     * <p>
     * Không đi qua API làm bài: luồng đó cần quiz, câu hỏi, bắt đầu bài, nộp bài — bốn bước không liên quan
     * gì tới thứ đang kiểm, mà mỗi bước lại là một chỗ có thể vỡ vì lý do khác.
     */
    private void taoMotCauTraLoiSai(String email) throws Exception {
        String userId = jdbc.queryForObject("select id::text from users where email = ?",
                String.class, email);

        String questionId = jdbc.queryForObject("""
                insert into questions (id, owner_id, type, content, explanation, difficulty, topic, points, source)
                values (gen_random_uuid(), ?::uuid, 'SINGLE_CHOICE', 'Câu hỏi bị trả lời sai',
                        'Vì sao đáp án đó đúng', 'MEDIUM', 'Chủ đề ôn', 1, 'MANUAL')
                returning id::text
                """, String.class, userId);
        jdbc.update("""
                insert into question_options (id, question_id, content, is_correct, order_index)
                values (gen_random_uuid(), ?::uuid, 'Đáp án đúng', true, 0),
                       (gen_random_uuid(), ?::uuid, 'Đáp án sai',  false, 1)
                """, questionId, questionId);

        String quizId = jdbc.queryForObject("""
                insert into quizzes (id, owner_id, title, visibility, difficulty)
                values (gen_random_uuid(), ?::uuid, 'Quiz để tạo câu sai', 'PRIVATE', 'MEDIUM')
                returning id::text
                """, String.class, userId);
        String attemptId = jdbc.queryForObject("""
                insert into quiz_attempts (id, user_id, quiz_id, mode, status, started_at, max_score)
                values (gen_random_uuid(), ?::uuid, ?::uuid, 'PRACTICE', 'SUBMITTED', now(), 1)
                returning id::text
                """, String.class, userId, quizId);
        jdbc.update("""
                insert into attempt_answers (id, attempt_id, question_id, is_correct, score, max_score,
                                            graded_by, answered_at)
                values (gen_random_uuid(), ?::uuid, ?::uuid, false, 0, 1, 'AUTO', now())
                """, attemptId, questionId);
    }
}
