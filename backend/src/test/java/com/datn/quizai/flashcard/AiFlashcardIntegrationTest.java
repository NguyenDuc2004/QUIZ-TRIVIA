package com.datn.quizai.flashcard;

import com.datn.quizai.ai.provider.AiCompletion;
import com.datn.quizai.ai.provider.AiOrchestrator;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sinh thẻ ghi nhớ bằng AI từ học liệu (features/11, FR-38).
 * <p>
 * {@code AiOrchestrator} bị giả lập: test này kiểm <b>luồng</b> quanh mô hình, không kiểm chất lượng đầu ra
 * của mô hình. Việc lọc đầu ra đã có {@code FlashcardJsonParserTest} kiểm riêng.
 * <p>
 * Bốn nhóm phép kiểm, mỗi nhóm nhắm một cách hỏng cụ thể:
 * <ol>
 *   <li><b>Người học dùng được</b> — endpoint không được nằm sau chốt CREATOR/ADMIN, vì cả tính năng thẻ
 *       ghi nhớ là của người học.</li>
 *   <li><b>Thẻ không tự vào bộ</b> — phải qua bước duyệt, và chỉ thẻ được chọn mới được lưu.</li>
 *   <li><b>Bộ thẻ đích không đổi được giữa hai bước</b> — duyệt phải ghi vào đúng bộ đã kiểm quyền lúc gửi
 *       yêu cầu, không phải bộ nào client nói lúc duyệt.</li>
 *   <li><b>Mô hình hỏng thì job FAILED kèm lý do</b>, không kẹt mãi ở RUNNING.</li>
 * </ol>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AiFlashcardIntegrationTest {

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

    @MockitoBean
    private AiOrchestrator aiOrchestrator;

    private String learnerToken;
    private String learnerId;

    @BeforeAll
    void setUp() throws Exception {
        learnerToken = register("the-ai-learner@example.com");
        learnerId = jdbc.queryForObject("select id::text from users where email = ?",
                String.class, "the-ai-learner@example.com");
    }

    @BeforeEach
    void resetMock() {
        reset(aiOrchestrator);
        embeddingReturns();
    }

    // ==================================================== 1. Người học dùng được

    @Test
    @DisplayName("LEARNER sinh được thẻ từ học liệu — endpoint KHÔNG nằm sau chốt CREATOR/ADMIN")
    void learnerCanGenerate() throws Exception {
        modelReturnsCards(3);
        String deckId = createDeck("Bộ nhận thẻ AI");
        String materialId = createReadyMaterial();

        String jobId = submitGenerate(deckId, materialId, 3);

        // Người học phải hỏi được cả trạng thái job. Để endpoint đó bên AiController (chốt CREATOR/ADMIN)
        // thì họ gửi được yêu cầu nhưng không lấy được kết quả — tệ hơn là không cho gửi.
        mockMvc.perform(get("/api/v1/flashcards/jobs/{id}", jobId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + learnerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"));
    }

    // ==================================================== 2. Phải qua bước duyệt

    @Test
    @DisplayName("Job xong KHÔNG tự lưu thẻ vào bộ — phải duyệt, và chỉ thẻ được chọn mới lưu")
    void generatedCardsNeedApproval() throws Exception {
        modelReturnsCards(3);
        String deckId = createDeck("Bộ kiểm bước duyệt");
        String materialId = createReadyMaterial();
        String jobId = submitGenerate(deckId, materialId, 3);

        // Chốt quan trọng nhất của cả luồng: thẻ chưa vào bộ. Một thẻ sai lọt vào sẽ được ôn lại hàng chục
        // lần theo lịch SRS, tức được học thuộc.
        assertThat(demSoThe(deckId)).as("thẻ không được tự vào bộ khi job xong").isZero();

        mockMvc.perform(post("/api/v1/flashcards/jobs/{id}/approve", jobId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + learnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"indexes\":[0,2]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.soThe").value(2));

        assertThat(demSoThe(deckId)).isEqualTo(2);

        // Thẻ đã lưu phải có nguồn AI_GENERATED và có trạng thái ôn ngay — nếu không nó chỉ nằm trong danh
        // sách mà không bao giờ vào phiên ôn
        assertThat(jdbc.queryForObject("""
                select count(*) from flashcards c
                  join flashcard_reviews r on r.flashcard_id = c.id
                where c.deck_id = ?::uuid and c.source = 'AI_GENERATED' and r.due_date = current_date
                """, Long.class, deckId)).isEqualTo(2);
    }

    @Test
    @DisplayName("Chọn chỉ số ngoài phạm vi thì bị loại, không ném lỗi 500")
    void outOfRangeIndexesAreIgnored() throws Exception {
        modelReturnsCards(2);
        String deckId = createDeck("Bộ kiểm chỉ số");
        String jobId = submitGenerate(deckId, createReadyMaterial(), 2);

        mockMvc.perform(post("/api/v1/flashcards/jobs/{id}/approve", jobId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + learnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"indexes\":[0,99,-1,0]}"))
                .andExpect(status().isCreated())
                // 99 và -1 bị loại, 0 lặp hai lần chỉ tính một → đúng 1 thẻ
                .andExpect(jsonPath("$.soThe").value(1));
    }

    // ==================================================== 3. Không đổi được bộ thẻ đích

    @Test
    @DisplayName("Bộ thẻ của người khác trả 404 ngay khi gửi yêu cầu, KHÔNG tạo job và KHÔNG gọi mô hình")
    void cannotGenerateIntoSomeoneElsesDeck() throws Exception {
        modelReturnsCards(2);
        String emailKhac = "the-ai-nguoi-khac@example.com";
        String tokenKhac = register(emailKhac);
        String idKhac = jdbc.queryForObject("select id::text from users where email = ?",
                String.class, emailKhac);
        String deckCuaNguoiKhac = createDeck("Bộ của người khác");   // thuộc learner chính

        mockMvc.perform(post("/api/v1/decks/{id}/cards/generate", deckCuaNguoiKhac)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenKhac)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"materialId\":\"%s\",\"count\":2}".formatted(UUID.randomUUID())))
                .andExpect(status().isNotFound());

        // Kiểm quyền phải xảy ra TRƯỚC khi tạo job và gọi mô hình: sinh xong rồi mới phát hiện không lưu
        // được là đã tốn tiền API cho một kết quả bỏ đi.
        //
        // Đếm theo ĐÚNG người vừa gửi yêu cầu, không đếm toàn bảng: các ca khác trong lớp này cũng tạo job
        // nên đếm toàn bảng thì con số không nói được điều gì về ca này.
        assertThat(jdbc.queryForObject("""
                select count(*) from ai_jobs where user_id = ?::uuid and type = 'GENERATE_FLASHCARDS'
                """, Long.class, idKhac))
                .as("không được tạo job khi bộ thẻ không thuộc người gửi").isZero();

        // Và mô hình không bị gọi lần nào cho lượt này
        org.mockito.Mockito.verify(aiOrchestrator, org.mockito.Mockito.never())
                .complete(any(), anyString(), any(), anyBoolean());
    }

    @Test
    @DisplayName("Job của người khác trả 404 khi duyệt")
    void cannotApproveSomeoneElsesJob() throws Exception {
        modelReturnsCards(2);
        String deckId = createDeck("Bộ để kiểm duyệt chéo");
        String jobId = submitGenerate(deckId, createReadyMaterial(), 2);

        String tokenKhac = register("the-ai-duyet-cheo@example.com");
        mockMvc.perform(post("/api/v1/flashcards/jobs/{id}/approve", jobId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenKhac)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"indexes\":[0]}"))
                .andExpect(status().isNotFound());
    }

    // ==================================================== 4. Mô hình hỏng

    @Test
    @DisplayName("Mô hình lỗi: job chuyển FAILED kèm lý do, KHÔNG kẹt ở RUNNING")
    void modelFailureMarksJobFailed() throws Exception {
        embeddingReturns();
        willThrow(new IllegalStateException("Hết hạn mức"))
                .given(aiOrchestrator).complete(any(), anyString(), any(), anyBoolean());

        String deckId = createDeck("Bộ khi mô hình lỗi");
        String jobId = submitGenerate(deckId, createReadyMaterial(), 2);

        mockMvc.perform(get("/api/v1/flashcards/jobs/{id}", jobId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + learnerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.errorMessage").exists());

        assertThat(demSoThe(deckId)).isZero();
    }

    @Test
    @DisplayName("Chưa có học liệu nào sẵn sàng: job FAILED kèm hướng dẫn, không phải lỗi kỹ thuật")
    void noMaterialGivesActionableFailure() throws Exception {
        modelReturnsCards(2);
        String deckId = createDeck("Bộ không có học liệu");

        // materialId không tồn tại → truy xuất rỗng
        String jobId = submitGenerate(deckId, UUID.randomUUID().toString(), 2);

        String body = mockMvc.perform(get("/api/v1/flashcards/jobs/{id}", jobId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + learnerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andReturn().getResponse().getContentAsString();

        // Thông báo phải nói người dùng làm gì tiếp, không phải "Sinh thẻ thất bại, thử lại sau"
        assertThat(objectMapper.readTree(body).get("errorMessage").asText())
                .contains("học liệu");
    }

    @Test
    @DisplayName("Duyệt job chưa xong bị chặn 409")
    void cannotApproveUnfinishedJob() throws Exception {
        String deckId = createDeck("Bộ job chưa xong");
        // Tạo job thẳng ở trạng thái RUNNING, không đi qua luồng nền
        String jobId = UUID.randomUUID().toString();
        // `request` là cột JSONB nên tham số text phải cast tường minh, không tự suy kiểu được
        jdbc.update("""
                insert into ai_jobs (id, user_id, type, status, request, created_at)
                values (?::uuid, ?::uuid, 'GENERATE_FLASHCARDS', 'RUNNING', cast(? as jsonb), now())
                """, jobId, learnerId,
                "{\"materialId\":\"%s\",\"deckId\":\"%s\",\"count\":2}"
                        .formatted(UUID.randomUUID(), deckId));

        mockMvc.perform(post("/api/v1/flashcards/jobs/{id}/approve", jobId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + learnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"indexes\":[0]}"))
                .andExpect(status().isConflict());
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

    private String createDeck(String title) throws Exception {
        String body = mockMvc.perform(post("/api/v1/decks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + learnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"%s\"}".formatted(title)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    /**
     * Dựng thẳng một học liệu đã READY kèm một đoạn có vector nhúng.
     * <p>
     * Không đi qua API nạp học liệu: luồng đó cần trích văn bản, cắt đoạn và job nền — ba bước không liên
     * quan gì tới thứ đang kiểm, mà mỗi bước lại là một chỗ có thể vỡ vì lý do khác.
     */
    private String createReadyMaterial() {
        String materialId = jdbc.queryForObject("""
                insert into learning_materials (id, owner_id, title, source_type, status, shared, created_at)
                values (gen_random_uuid(), ?::uuid, 'Tài liệu kiểm thử', 'TEXT', 'READY', false, now())
                returning id::text
                """, String.class, learnerId);

        String vector = "[" + "0.01,".repeat(767) + "0.01]";
        jdbc.update("""
                insert into material_chunks (id, material_id, chunk_index, content, embedding)
                values (gen_random_uuid(), ?::uuid, 0,
                        'HTTP 404 nghĩa là không tìm thấy tài nguyên. HTTP 500 là lỗi phía máy chủ.',
                        cast(? as vector))
                """, materialId, vector);
        return materialId;
    }

    /** Gửi yêu cầu sinh thẻ và trả jobId. Job chạy đồng bộ trong test vì luồng nền dùng cùng transaction. */
    private String submitGenerate(String deckId, String materialId, int count) throws Exception {
        String body = mockMvc.perform(post("/api/v1/decks/{id}/cards/generate", deckId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + learnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"materialId\":\"%s\",\"count\":%d}".formatted(materialId, count)))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();
        String jobId = objectMapper.readTree(body).get("id").asText();
        awaitJobDone(jobId);
        return jobId;
    }

    /** Chờ job nền xong. Không chờ thì phép kiểm đọc trạng thái lúc job còn PENDING và xanh/đỏ tuỳ máy. */
    private void awaitJobDone(String jobId) throws Exception {
        long deadline = System.currentTimeMillis() + 15_000;
        while (System.currentTimeMillis() < deadline) {
            String status = jdbc.queryForObject("select status from ai_jobs where id = ?::uuid",
                    String.class, jobId);
            if ("SUCCEEDED".equals(status) || "FAILED".equals(status)) {
                return;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Job sinh thẻ không xong sau 15 giây");
    }

    private long demSoThe(String deckId) {
        Long n = jdbc.queryForObject("select count(*) from flashcards where deck_id = ?::uuid",
                Long.class, deckId);
        return n == null ? 0 : n;
    }

    private void modelReturnsCards(int count) {
        StringBuilder sb = new StringBuilder("{\"flashcards\":[");
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append("{\"front\":\"Mã trạng thái %d nghĩa là gì?\",\"back\":\"Ý nghĩa %d\"}"
                    .formatted(400 + i, i));
        }
        sb.append("]}");

        AiCompletion completion = new AiCompletion("gemini", "gemini-test", sb.toString(), 0, 0, 0);
        given(aiOrchestrator.complete(any(), anyString(), any())).willReturn(completion);
        given(aiOrchestrator.complete(any(), anyString(), any(), anyBoolean())).willReturn(completion);
    }

    /** Vector giả đúng 768 chiều của cột `material_chunks.embedding`. */
    private void embeddingReturns() {
        List<Float> vector = new ArrayList<>(768);
        for (int i = 0; i < 768; i++) {
            vector.add(0.01f);
        }
        given(aiOrchestrator.embed(anyString(), any())).willReturn(vector);
        given(aiOrchestrator.embed(anyString(), any(), anyBoolean())).willReturn(vector);
    }
}
