package com.datn.quizai.ai;

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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test luồng nạp học liệu và sinh đề (docs/features/05) trên PostgreSQL + Redis thật.
 * <p>
 * <b>Vì sao viết muộn:</b> lát cắt 5 chỉ có test cho hai lớp thuần logic ({@code QuestionJsonParser},
 * {@code TextChunker}); toàn bộ phần <i>luồng</i> — nạp học liệu, chạy job, duyệt câu — chỉ được
 * kiểm bằng bộ nghiệm thu chạy tay với Gemini thật. Nghĩa là khi Gemini hết hạn mức thì không còn
 * cách nào biết luồng có vỡ hay không. Lát cắt 6 sửa vào đúng hai lớp đó (cho phép chờ lâu hơn khi
 * vướng hạn mức), nên phải có test không phụ thuộc mạng ngoài.
 * <p>
 * {@link AiOrchestrator} bị mock: gọi mô hình thật khiến test phụ thuộc mạng, tốn tiền và trả kết
 * quả khác nhau mỗi lần. Phần <i>chất lượng câu hỏi</i> vẫn đo bằng bộ nghiệm thu tay; ở đây kiểm
 * <b>cơ chế</b>: job có chạy không, hỏng thì trạng thái có dừng đúng chỗ không, ai đọc được dữ liệu
 * của ai, và Creator có phải duyệt trước khi câu vào ngân hàng không.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AiGenerationIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    /** Đủ dài để qua ràng buộc "ít nhất 100 ký tự" và cắt được thành nhiều đoạn. */
    private static final String MATERIAL_TEXT = ("""
            Giao thức HTTP là nền tảng của World Wide Web. HTTP hoạt động theo mô hình yêu cầu và
            phản hồi: máy khách gửi yêu cầu, máy chủ trả về phản hồi kèm mã trạng thái. Mã 200 nghĩa
            là thành công, 404 nghĩa là không tìm thấy tài nguyên, 500 nghĩa là máy chủ gặp lỗi.
            HTTP là giao thức phi trạng thái, tức là máy chủ không nhớ gì về yêu cầu trước đó.
            """).repeat(4);

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AiOrchestrator aiOrchestrator;

    private String creatorToken;
    private String otherCreatorToken;
    private String learnerToken;

    @BeforeAll
    void registerUsers() throws Exception {
        creatorToken = register("creator-gen@example.com", "CREATOR");
        otherCreatorToken = register("creator-gen-khac@example.com", "CREATOR");
        learnerToken = register("learner-gen@example.com", "LEARNER");
    }

    // ================================================================ nạp học liệu

    @Test
    @DisplayName("Nạp học liệu: trả 202 ngay, cắt đoạn và sinh embedding ở luồng nền rồi chuyển READY")
    void shouldIngestMaterialInBackground() throws Exception {
        embeddingReturns();

        String materialId = createMaterial("Giao thức HTTP");

        // 202 chứ không phải 201: việc thật còn đang chạy, chưa xong lúc trả lời
        JsonNode ready = awaitMaterialStatus(materialId, "READY");
        assertThat(ready.get("chunkCount").asInt()).isPositive();
        assertThat(ready.get("charCount").asInt()).isEqualTo(MATERIAL_TEXT.length());
        assertThat(ready.get("errorMessage").isNull()).isTrue();
    }

    @Test
    @DisplayName("Sinh embedding hỏng: học liệu chuyển FAILED kèm lý do, KHÔNG kẹt mãi ở PROCESSING")
    void shouldMarkMaterialFailedWhenEmbeddingBreaks() throws Exception {
        reset(aiOrchestrator);
        willThrow(new IllegalStateException("Hết hạn mức embedding"))
                .given(aiOrchestrator).embed(anyString(), any(), anyBoolean());

        String materialId = createMaterial("Tài liệu hỏng");

        JsonNode failed = awaitMaterialStatus(materialId, "FAILED");
        // Không có trạng thái dừng thì người dùng nhìn "đang xử lý" vĩnh viễn
        assertThat(failed.get("errorMessage").asText()).contains("hạn mức");
    }

    @Test
    @DisplayName("Học liệu là dữ liệu riêng: người khác đọc hay xoá đều nhận 404")
    void shouldIsolateMaterialsBetweenAccounts() throws Exception {
        embeddingReturns();
        String materialId = createMaterial("Tài liệu riêng");

        // 404 chứ không phải 403: người khác không được biết tài liệu này tồn tại
        mockMvc.perform(get("/api/v1/ai/materials/{id}", materialId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherCreatorToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/v1/ai/materials/{id}", materialId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherCreatorToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Learner không SINH ĐỀ được — đó là công cụ soạn nội dung, và mỗi lời gọi đều tốn tiền")
    void shouldRejectLearnerFromGenerationEndpoints() throws Exception {
        mockMvc.perform(post("/api/v1/ai/generate-questions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + learnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"topic\":\"HTTP\",\"count\":3}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Learner XEM ĐƯỢC trạng thái dịch vụ AI — việc nạp học liệu của họ phụ thuộc vào nó")
    void shouldLetLearnerReadAiStatus() throws Exception {
        // Endpoint này từng nằm trong `AiController` nên trả 403 với người học. Hệ quả: trang Học liệu
        // của họ KHÔNG BAO GIỜ hiện được cảnh báo "chưa cấu hình API key" — họ tải tệp lên, tệp dừng ở
        // trạng thái Lỗi, và không có gì cho biết vì sao.
        mockMvc.perform(get("/api/v1/ai/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + learnerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").exists());
    }

    @Test
    @DisplayName("Learner NẠP được học liệu của chính mình — ranh giới dời từ vai trò sang trần số lượng")
    void shouldLetLearnerCreateMaterial() throws Exception {
        // Ca này từng khẳng định điều ngược lại. Nó không sai lúc viết: luật khi đó là khoá cả cụm
        // `/ai/**` theo vai trò, với lý do chi phí. Nhưng khoá theo vai trò làm trợ lý học tập chết
        // hẳn với người học đơn lẻ (features/08), nên phần canh chi phí chuyển sang trần số tài liệu
        // — thứ đo đúng đại lượng cần đo. Sinh đề thì vẫn khoá, và ca ở trên giữ đúng phần đó.
        mockMvc.perform(post("/api/v1/ai/materials")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + learnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(materialBody("Vở ghi của người học")))
                .andExpect(status().isAccepted());
    }

    // ================================================================ sinh đề

    @Test
    @DisplayName("Sinh đề theo chủ đề: job chạy nền, xong thì SUCCEEDED kèm câu hỏi nháp")
    void shouldGenerateQuestionsFromTopic() throws Exception {
        modelReturnsQuestions(3);

        String jobId = requestGeneration("{\"topic\":\"mã trạng thái HTTP\",\"count\":3}");

        JsonNode questions = generatedQuestionsOf(awaitJobStatus(jobId, "SUCCEEDED"));
        assertThat(questions).hasSize(3);
        assertThat(questions.get(0).get("content").asText()).isNotBlank();
    }

    @Test
    @DisplayName("Sinh đề bám học liệu: có dùng tới vector của tài liệu")
    void shouldGenerateGroundedOnMaterial() throws Exception {
        embeddingReturns();
        String materialId = createMaterial("Học liệu cho sinh đề");
        awaitMaterialStatus(materialId, "READY");

        modelReturnsQuestions(2);
        String jobId = requestGeneration("""
                {"topic":"HTTP","count":2,"materialId":"%s","useMaterials":true}
                """.formatted(materialId));

        assertThat(generatedQuestionsOf(awaitJobStatus(jobId, "SUCCEEDED"))).hasSize(2);
    }

    @Test
    @DisplayName("Mô hình lỗi: job chuyển FAILED kèm thông điệp, không kẹt ở PENDING")
    void shouldMarkJobFailedWhenModelBreaks() throws Exception {
        reset(aiOrchestrator);
        willThrow(new IllegalStateException("Dịch vụ AI đang quá tải hạn mức"))
                .given(aiOrchestrator).complete(any(), anyString(), any(), anyBoolean());

        String jobId = requestGeneration("{\"topic\":\"HTTP\",\"count\":3}");

        JsonNode failed = awaitJobStatus(jobId, "FAILED");
        assertThat(failed.get("errorMessage").asText()).isNotBlank();
    }

    @Test
    @DisplayName("Mô hình trả JSON hỏng: job FAILED chứ không lưu rác vào ngân hàng câu hỏi")
    void shouldRejectUnusableModelOutput() throws Exception {
        reset(aiOrchestrator);
        modelReturns("đây không phải JSON");

        String jobId = requestGeneration("{\"topic\":\"HTTP\",\"count\":3}");
        assertThat(awaitJobStatus(jobId, "FAILED")).isNotNull();
    }

    @Test
    @DisplayName("Job là dữ liệu riêng: người khác xem hay duyệt đều nhận 404")
    void shouldIsolateJobsBetweenAccounts() throws Exception {
        modelReturnsQuestions(2);
        String jobId = requestGeneration("{\"topic\":\"HTTP\",\"count\":2}");
        awaitJobStatus(jobId, "SUCCEEDED");

        mockMvc.perform(get("/api/v1/ai/jobs/{id}", jobId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherCreatorToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/ai/jobs/{id}/approve", jobId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherCreatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"indexes\":[0]}"))
                .andExpect(status().isNotFound());
    }

    // ================================================================ human-in-the-loop

    @Test
    @DisplayName("Câu AI sinh KHÔNG tự vào ngân hàng — chỉ câu Creator chọn mới được lưu")
    void shouldOnlySaveApprovedQuestions() throws Exception {
        modelReturnsQuestions(3);
        String jobId = requestGeneration("{\"topic\":\"HTTP duyệt\",\"count\":3}");
        awaitJobStatus(jobId, "SUCCEEDED");

        int bankBefore = countQuestionsInBank();

        // Chỉ chọn 2 trong 3 câu
        String body = mockMvc.perform(post("/api/v1/ai/jobs/{id}/approve", jobId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + creatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"indexes\":[0,2]}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        assertThat(objectMapper.readTree(body)).hasSize(2);
        assertThat(countQuestionsInBank()).isEqualTo(bankBefore + 2);

        // Nguồn gốc phải ghi rõ là AI sinh, để sau này lọc/thống kê được
        assertThat(objectMapper.readTree(body).get(0).get("source").asText())
                .isEqualTo("AI_GENERATED");
    }

    @Test
    @DisplayName("Duyệt câu nằm ngoài danh sách trả 400, không lưu gì")
    void shouldRejectOutOfRangeApproval() throws Exception {
        modelReturnsQuestions(2);
        String jobId = requestGeneration("{\"topic\":\"HTTP ngoài phạm vi\",\"count\":2}");
        awaitJobStatus(jobId, "SUCCEEDED");

        int bankBefore = countQuestionsInBank();
        mockMvc.perform(post("/api/v1/ai/jobs/{id}/approve", jobId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + creatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"indexes\":[99]}"))
                .andExpect(status().isBadRequest());

        assertThat(countQuestionsInBank()).isEqualTo(bankBefore);
    }

    @Test
    @DisplayName("Chặn xin quá 20 câu mỗi lần — mỗi câu là tiền")
    void shouldCapQuestionCount() throws Exception {
        mockMvc.perform(post("/api/v1/ai/generate-questions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + creatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"topic\":\"HTTP\",\"count\":50}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.count").exists());
    }

    // ================================================================ helper

    /**
     * Cả sinh đề lẫn embedding đều chạy trong job nền nên dùng bản {@code background = true} —
     * bản đó chờ được hết cửa sổ hạn mức thay vì bỏ cuộc sau vài giây (features/06).
     * Stub cả hai bản để test không phụ thuộc vào việc code gọi bản nào.
     */
    private void modelReturns(String text) {
        AiCompletion completion = new AiCompletion("gemini", "gemini-test", text, 0, 0, 0);
        given(aiOrchestrator.complete(any(), anyString(), any())).willReturn(completion);
        given(aiOrchestrator.complete(any(), anyString(), any(), anyBoolean())).willReturn(completion);
    }

    private void modelReturnsQuestions(int count) {
        reset(aiOrchestrator);
        embeddingReturns();

        String questions = IntStream.range(0, count)
                .mapToObj(i -> """
                        {"type":"SINGLE_CHOICE","question":"Mã trạng thái %d nghĩa là gì?",
                         "options":["Thành công","Không tìm thấy","Lỗi máy chủ","Chuyển hướng"],
                         "correctAnswer":"Thành công","explanation":"Giải thích %d",
                         "difficulty":"EASY","topic":"HTTP"}""".formatted(200 + i, i))
                .collect(Collectors.joining(","));
        modelReturns("{\"questions\":[" + questions + "]}");
    }

    /** Vector giả đúng số chiều của cột `material_chunks.embedding`. */
    private void embeddingReturns() {
        List<Float> vector = new ArrayList<>(768);
        for (int i = 0; i < 768; i++) {
            vector.add(0.01f);
        }
        given(aiOrchestrator.embed(anyString(), any())).willReturn(vector);
        given(aiOrchestrator.embed(anyString(), any(), anyBoolean())).willReturn(vector);
    }

    private String createMaterial(String title) throws Exception {
        String body = mockMvc.perform(post("/api/v1/ai/materials")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + creatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(materialBody(title)))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    private String materialBody(String title) throws Exception {
        return objectMapper.writeValueAsString(java.util.Map.of(
                "title", title, "topic", "HTTP", "content", MATERIAL_TEXT));
    }

    private String requestGeneration(String json) throws Exception {
        String body = mockMvc.perform(post("/api/v1/ai/generate-questions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + creatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    /**
     * Chờ luồng nền chạy xong bằng cách hỏi lại trạng thái, không phải {@code sleep} một khoảng cố
     * định — chọn ngắn thì test chập chờn trên máy chậm, chọn dài thì cả bộ test lê thê.
     */
    private JsonNode awaitMaterialStatus(String materialId, String expected) throws Exception {
        return await("/api/v1/ai/materials/" + materialId, expected);
    }

    private JsonNode awaitJobStatus(String jobId, String expected) throws Exception {
        return await("/api/v1/ai/jobs/" + jobId, expected);
    }

    private JsonNode await(String path, String expectedStatus) throws Exception {
        long deadline = System.currentTimeMillis() + 20_000;
        String last = "";
        while (System.currentTimeMillis() < deadline) {
            String body = mockMvc.perform(get(path)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + creatorToken))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            JsonNode node = objectMapper.readTree(body);
            last = node.get("status").asText();
            if (expectedStatus.equals(last)) {
                return node;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Chờ mãi không thấy trạng thái " + expectedStatus + ", hiện là " + last);
    }

    /** Kết quả job nằm trong trường `result` dạng JSON thô, không phải một trường riêng. */
    private JsonNode generatedQuestionsOf(JsonNode job) {
        return job.get("result").get("questions");
    }

    private int countQuestionsInBank() throws Exception {
        String body = mockMvc.perform(get("/api/v1/questions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + creatorToken)
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("totalElements").asInt();
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
