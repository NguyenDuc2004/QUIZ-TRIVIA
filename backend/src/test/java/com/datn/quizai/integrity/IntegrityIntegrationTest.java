package com.datn.quizai.integrity;

import com.datn.quizai.ai.provider.AiCompletion;
import com.datn.quizai.ai.provider.AiOrchestrator;
import com.datn.quizai.attempt.service.AttemptSubmittedEvent;
import com.datn.quizai.integrity.service.IntegrityEventListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Chống gian lận thi (features/12).
 * <p>
 * Phép tính điểm rủi ro đã có {@code RiskScorerTest} kiểm riêng. Lớp này kiểm <b>ba ràng buộc đạo đức</b> của
 * đặc tả — chúng quan trọng hơn con số, vì vi phạm chúng là vi phạm quyền của người học:
 * <ol>
 *   <li><b>Chỉ chế độ thi.</b> Lượt luyện tập bị từ chối ghi tín hiệu.</li>
 *   <li><b>Không lưu nội dung.</b> Client gửi kèm văn bản đã dán thì văn bản đó không vào cơ sở dữ liệu.</li>
 *   <li><b>Không tự kết luận.</b> Bài bị gắn cờ luôn ở {@code PENDING}; chỉ người thật chuyển được.</li>
 * </ol>
 * Cộng một nhóm nữa về <b>quyền xem</b>: người làm bài không xem được điểm rủi ro của chính mình.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IntegrityIntegrationTest {

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
    @Autowired
    private IntegrityEventListener listener;

    @MockitoBean
    private AiOrchestrator aiOrchestrator;

    private String tokenHocVien;
    private UUID idHocVien;
    private String tokenChuQuiz;
    private UUID idChuQuiz;

    @BeforeAll
    void setUp() throws Exception {
        tokenHocVien = register("lt-hocvien@example.com", "LEARNER");
        idHocVien = idOf("lt-hocvien@example.com");
        tokenChuQuiz = register("lt-chuquiz@example.com", "CREATOR");
        idChuQuiz = idOf("lt-chuquiz@example.com");
    }

    @BeforeEach
    void resetMock() {
        reset(aiOrchestrator);
        given(aiOrchestrator.complete(any(), anyString(), any()))
                .willReturn(new AiCompletion("gemini", "gemini-test",
                        "Có một số tín hiệu đáng chú ý, nhưng đều có thể giải thích vô hại.", 0, 0, 0));
    }

    // ==================================================== 1. Chỉ chế độ thi

    @Test
    @DisplayName("Lượt LUYỆN TẬP bị từ chối ghi tín hiệu, kèm thông báo nói rõ vì sao")
    void practiceAttemptIsRejected() throws Exception {
        UUID attempt = taoLuot(idHocVien, idChuQuiz, "PRACTICE");

        String body = mockMvc.perform(post("/api/v1/attempts/{id}/proctoring-events", attempt)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenHocVien)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"events\":[{\"type\":\"TAB_HIDDEN\"}]}"))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        // Luyện tập không phải chỗ theo dõi hành vi. Thông báo phải nói rõ để người dùng biết mình KHÔNG bị
        // theo dõi khi luyện tập, chứ không chỉ ăn một lỗi 400 vô nghĩa.
        assertThat(body).contains("luyện tập");
        assertThat(demSuKien(attempt)).isZero();
    }

    @Test
    @DisplayName("Không gửi được tín hiệu vào lượt thi của người khác")
    void cannotSendEventsToSomeoneElsesAttempt() throws Exception {
        UUID cuaNguoiKhac = taoLuot(idChuQuiz, idChuQuiz, "EXAM");

        // Nếu gửi được thì bất kỳ ai cũng hạ được điểm tin cậy bài của người khác
        mockMvc.perform(post("/api/v1/attempts/{id}/proctoring-events", cuaNguoiKhac)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenHocVien)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"events\":[{\"type\":\"PASTE\",\"length\":900}]}"))
                .andExpect(status().isNotFound());

        assertThat(demSuKien(cuaNguoiKhac)).isZero();
    }

    // ==================================================== 2. Không lưu nội dung

    @Test
    @DisplayName("Client gửi kèm nội dung đã dán: nội dung KHÔNG vào cơ sở dữ liệu, chỉ còn độ dài")
    void pastedTextIsNeverStored() throws Exception {
        UUID attempt = taoLuot(idHocVien, idChuQuiz, "EXAM");
        String noiDungBiMat = "DAP_AN_LAY_TU_NOI_KHAC_12345";

        mockMvc.perform(post("/api/v1/attempts/{id}/proctoring-events", attempt)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenHocVien)
                        .contentType(MediaType.APPLICATION_JSON)
                        // Client cố tình gửi thêm trường `text` — server phải bỏ nó
                        .content("""
                                {"events":[{"type":"PASTE","length":28,"text":"%s"}]}
                                """.formatted(noiDungBiMat)))
                .andExpect(status().isOk());

        String detail = jdbc.queryForObject(
                "select detail::text from proctoring_events where attempt_id = ?", String.class, attempt);

        // Đây là phép kiểm quan trọng nhất của lớp: ghi nội dung dán là thu dữ liệu ngoài phạm vi bài thi,
        // điều đặc tả cấm. Server dựng `detail` từ danh sách trường vô hại, không lưu lại payload client gửi.
        assertThat(detail).contains("length").doesNotContain(noiDungBiMat).doesNotContain("text");
    }

    @Test
    @DisplayName("Mốc thời gian ở TƯƠNG LAI bị kéo về hiện tại — client giả mạo được trường này")
    void futureTimestampIsClamped() throws Exception {
        UUID attempt = taoLuot(idHocVien, idChuQuiz, "EXAM");

        mockMvc.perform(post("/api/v1/attempts/{id}/proctoring-events", attempt)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenHocVien)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"events\":[{\"type\":\"TAB_HIDDEN\",\"occurredAt\":\"2099-01-01T00:00:00Z\"}]}"))
                .andExpect(status().isOk());

        // Không chặn thì mọi thống kê theo thời gian đều lệch, và đây là trường dễ giả mạo nhất trong payload.
        //
        // So với `now() + 1 phút` chứ không phải `now()` trơn: mốc bị kéo về theo đồng hồ của JVM, còn
        // `now()` là đồng hồ bên trong container PostgreSQL — hai đồng hồ khác nhau và lệch nhau vài trăm
        // milli-giây theo cả hai chiều (đo được 276ms trên máy này). Test so hai đồng hồ đó bằng dấu `<=`
        // sẽ đỏ ngẫu nhiên vì môi trường, không vì sản phẩm; đã đỏ thật một lần trong lần chạy đầy đủ.
        //
        // Biên một phút vẫn loại được mốc 2099 một cách dứt khoát — thứ phép kiểm này thật sự quan tâm.
        assertThat(jdbc.queryForObject("""
                select occurred_at <= now() + interval '1 minute' from proctoring_events
                 where attempt_id = ?
                """, Boolean.class, attempt))
                .as("mốc 2099 phải bị kéo về hiện tại")
                .isTrue();
    }

    // ==================================================== 3. Không tự kết luận

    @Test
    @DisplayName("Bài nhiều tín hiệu bị gắn cờ nhưng trạng thái vẫn PENDING — hệ thống không tự kết luận")
    void flaggedAttemptStaysPending() throws Exception {
        UUID attempt = taoLuot(idHocVien, idChuQuiz, "EXAM");
        guiNhieuTinHieu(attempt);

        listener.onAttemptSubmitted(new AttemptSubmittedEvent(attempt, idHocVien));

        String body = mockMvc.perform(get("/api/v1/attempts/{id}/integrity", attempt)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenChuQuiz))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.biGanCo").value(true))
                // Đây là chốt đạo đức: điểm cao là lý do để người thật xem, KHÔNG phải kết luận
                .andExpect(jsonPath("$.reviewStatus").value("PENDING"))
                .andReturn().getResponse().getContentAsString();

        var json = objectMapper.readTree(body);
        assertThat(json.get("riskScore").asInt()).isGreaterThanOrEqualTo(60);
        // Cờ phải nói lý do cụ thể, không chỉ một con số
        assertThat(json.get("flags")).isNotEmpty();
        // Và báo cáo luôn kèm câu nhắc rằng tín hiệu giả mạo được
        assertThat(json.get("canhBao").asText()).contains("giả mạo");
    }

    @Test
    @DisplayName("Không ai đặt được trạng thái về PENDING — đó là trạng thái của hệ thống, không phải kết luận")
    void cannotSetStatusBackToPending() throws Exception {
        UUID attempt = taoLuot(idHocVien, idChuQuiz, "EXAM");
        guiNhieuTinHieu(attempt);
        listener.onAttemptSubmitted(new AttemptSubmittedEvent(attempt, idHocVien));

        mockMvc.perform(put("/api/v1/attempts/{id}/integrity/review", attempt)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenChuQuiz)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PENDING\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Chủ quiz kết luận được, và kết luận ghi lại ai làm cùng lý do")
    void ownerCanReview() throws Exception {
        UUID attempt = taoLuot(idHocVien, idChuQuiz, "EXAM");
        guiNhieuTinHieu(attempt);
        listener.onAttemptSubmitted(new AttemptSubmittedEvent(attempt, idHocVien));

        mockMvc.perform(put("/api/v1/attempts/{id}/integrity/review", attempt)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenChuQuiz)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"VALID\",\"note\":\"Em có nhắn trước là mạng chập chờn\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewStatus").value("VALID"))
                .andExpect(jsonPath("$.reviewNote").exists());

        // Ghi lại người kết luận: một kết luận không biết ai đưa ra thì không quy trách nhiệm được
        assertThat(jdbc.queryForObject("""
                select reviewed_by::text from attempt_integrity where attempt_id = ?
                """, String.class, attempt)).isEqualTo(idChuQuiz.toString());
    }

    // ==================================================== 4. Quyền xem

    @Test
    @DisplayName("Người làm bài KHÔNG xem được điểm rủi ro của chính mình")
    void examineeCannotSeeOwnRiskScore() throws Exception {
        UUID attempt = taoLuot(idHocVien, idChuQuiz, "EXAM");
        guiNhieuTinHieu(attempt);
        listener.onAttemptSubmitted(new AttemptSubmittedEvent(attempt, idHocVien));

        // Biết chính xác tín hiệu nào bị tính và nặng bao nhiêu là biết cách tránh, và khi đó cơ chế chỉ còn
        // lọc được người không biết nó tồn tại. Trả 404 chứ không 403 để không xác nhận bài đang bị gắn cờ.
        mockMvc.perform(get("/api/v1/attempts/{id}/integrity", attempt)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenHocVien))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Lượt luyện tập không có báo cáo tính toàn vẹn nào được tạo")
    void practiceAttemptHasNoIntegrityRecord() {
        UUID attempt = taoLuot(idHocVien, idChuQuiz, "PRACTICE");

        listener.onAttemptSubmitted(new AttemptSubmittedEvent(attempt, idHocVien));

        // Tạo một bản rỗng cho mỗi lượt luyện tập chỉ làm bảng phình và làm loãng danh sách rà soát
        assertThat(jdbc.queryForObject("""
                select count(*) from attempt_integrity where attempt_id = ?
                """, Long.class, attempt)).isZero();
    }

    @Test
    @DisplayName("Bài sạch: có bản tổng hợp, 0 điểm, không cờ, và KHÔNG gọi mô hình AI")
    void cleanAttemptDoesNotCallAi() {
        UUID attempt = taoLuot(idHocVien, idChuQuiz, "EXAM");

        listener.onAttemptSubmitted(new AttemptSubmittedEvent(attempt, idHocVien));

        assertThat(jdbc.queryForObject("select risk_score from attempt_integrity where attempt_id = ?",
                Integer.class, attempt)).isZero();
        // Gọi mô hình cho mọi lượt thi là tốn hạn mức cho hàng loạt bài mà kết luận đã biết trước
        org.mockito.Mockito.verify(aiOrchestrator, org.mockito.Mockito.never())
                .complete(any(), anyString(), any());
    }

    @Test
    @DisplayName("Khách chưa đăng nhập nhận 401")
    void guestIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/admin/integrity/flagged")).andExpect(status().isUnauthorized());
    }

    // ================================================================ helper

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

    // ============================================================ FR-48 — chế độ thi nghiêm ngặt

    @Test
    @DisplayName("Quiz bật chế độ nghiêm ngặt: lượt EXAM nhận strictExam = true")
    void shouldMarkExamAttemptAsStrict() throws Exception {
        UUID quiz = taoQuiz(idChuQuiz, true);
        UUID luot = taoLuotTrenQuiz(idHocVien, quiz, "EXAM");

        mockMvc.perform(get("/api/v1/attempts/{id}", luot)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenHocVien))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attempt.strictExam").value(true));
    }

    @Test
    @DisplayName("Cùng quiz đó nhưng lượt LUYỆN TẬP: strictExam = false")
    void shouldNotApplyStrictModeToPracticeAttempt() throws Exception {
        // Đây là luật quan trọng nhất của FR-48 và là chỗ dễ hỏng nhất: server trả cờ ĐÃ TÍNH cho lượt này
        // (`quiz.strictExam && mode == EXAM`), không trả cờ thô của quiz. Trả cờ thô thì frontend phải tự
        // nhớ nhân với chế độ ở mọi chỗ dùng, và một chỗ quên là người luyện tập bị ép toàn màn hình —
        // vi phạm thẳng ràng buộc "luyện tập không bị theo dõi" của đặc tả.
        UUID quiz = taoQuiz(idChuQuiz, true);
        UUID luot = taoLuotTrenQuiz(idHocVien, quiz, "PRACTICE");

        mockMvc.perform(get("/api/v1/attempts/{id}", luot)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenHocVien))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attempt.strictExam").value(false));
    }

    @Test
    @DisplayName("Quiz KHÔNG bật thì lượt EXAM cũng không nghiêm ngặt")
    void shouldNotApplyStrictModeWhenQuizDoesNotAskForIt() throws Exception {
        UUID quiz = taoQuiz(idChuQuiz, false);
        UUID luot = taoLuotTrenQuiz(idHocVien, quiz, "EXAM");

        mockMvc.perform(get("/api/v1/attempts/{id}", luot)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenHocVien))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attempt.strictExam").value(false));
    }

    @Test
    @DisplayName("Mặc định của quiz mới là TẮT — không đổi hành vi của quiz đang có")
    void shouldDefaultToDisabled() throws Exception {
        String body = mockMvc.perform(post("/api/v1/quizzes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenChuQuiz)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Quiz không khai strictExam\",\"difficulty\":\"EASY\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.strictExam").value(false))
                .andReturn().getResponse().getContentAsString();

        // Bật lên rồi cập nhật KHÔNG kèm trường đó: phải giữ nguyên, không âm thầm tắt
        String quizId = objectMapper.readTree(body).get("id").asText();
        mockMvc.perform(put("/api/v1/quizzes/{id}", quizId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenChuQuiz)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Quiz không khai strictExam\",\"strictExam\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.strictExam").value(true));

        mockMvc.perform(put("/api/v1/quizzes/{id}", quizId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenChuQuiz)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Sửa tiêu đề thôi\"}"))
                .andExpect(status().isOk())
                // Client cũ hoặc một form thiếu trường không được phép tắt cờ của chủ quiz
                .andExpect(jsonPath("$.strictExam").value(true));
    }

    /** Quiz có/không bật chế độ nghiêm ngặt. */
    private UUID taoQuiz(UUID chuQuiz, boolean nghiemNgat) {
        return UUID.fromString(jdbc.queryForObject("""
                insert into quizzes (id, owner_id, title, visibility, difficulty, strict_exam)
                values (gen_random_uuid(), ?, 'Đề thi FR-48', 'PRIVATE', 'MEDIUM', ?)
                returning id::text
                """, String.class, chuQuiz, nghiemNgat));
    }

    private UUID taoLuotTrenQuiz(UUID nguoiLam, UUID quizId, String mode) {
        return UUID.fromString(jdbc.queryForObject("""
                insert into quiz_attempts (id, user_id, quiz_id, mode, status, started_at, max_score)
                values (gen_random_uuid(), ?, ?, ?, 'IN_PROGRESS', now(), 10)
                returning id::text
                """, String.class, nguoiLam, quizId, mode));
    }

    private UUID idOf(String email) {
        return UUID.fromString(jdbc.queryForObject("select id::text from users where email = ?",
                String.class, email));
    }

    /** Dựng thẳng một lượt làm bài: luồng nộp bài đầy đủ không liên quan tới thứ đang kiểm. */
    private UUID taoLuot(UUID nguoiLam, UUID chuQuiz, String mode) {
        UUID quizId = UUID.fromString(jdbc.queryForObject("""
                insert into quizzes (id, owner_id, title, visibility, difficulty)
                values (gen_random_uuid(), ?, 'Đề thi kiểm thử', 'PRIVATE', 'MEDIUM')
                returning id::text
                """, String.class, chuQuiz));
        return UUID.fromString(jdbc.queryForObject("""
                insert into quiz_attempts (id, user_id, quiz_id, mode, status, started_at, max_score)
                values (gen_random_uuid(), ?, ?, ?, 'SUBMITTED', now(), 10)
                returning id::text
                """, String.class, nguoiLam, quizId, mode));
    }

    /** Gửi đủ tín hiệu để vượt ngưỡng gắn cờ. */
    private void guiNhieuTinHieu(UUID attempt) throws Exception {
        mockMvc.perform(post("/api/v1/attempts/{id}/proctoring-events", attempt)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenHocVien)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"events":[
                                  {"type":"TAB_HIDDEN"},{"type":"TAB_HIDDEN"},{"type":"TAB_HIDDEN"},
                                  {"type":"TAB_HIDDEN"},
                                  {"type":"PASTE","length":400},
                                  {"type":"ANSWER_TOO_FAST","seconds":2},
                                  {"type":"ANSWER_TOO_FAST","seconds":1}
                                ]}
                                """))
                .andExpect(status().isOk());
    }

    private long demSuKien(UUID attempt) {
        Long n = jdbc.queryForObject("select count(*) from proctoring_events where attempt_id = ?",
                Long.class, attempt);
        return n == null ? 0 : n;
    }
}
