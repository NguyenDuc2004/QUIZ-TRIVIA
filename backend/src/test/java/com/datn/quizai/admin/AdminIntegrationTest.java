package com.datn.quizai.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Khu vực quản trị (features/10).
 * <p>
 * Bốn nhóm phép kiểm, mỗi nhóm nhắm một cách hỏng cụ thể:
 * <ol>
 *   <li><b>Phân quyền</b> — người học và người tạo nội dung không được chạm vào bất cứ endpoint nào ở
 *       đây. Đây là lớp có `@PreAuthorize` cấp lớp, nên phải kiểm rằng nó thật sự có hiệu lực.</li>
 *   <li><b>Khoá tài khoản thực sự chặn được</b> — không chỉ đặt cờ. Kiểm cả lối `login` và lối
 *       `refresh`: chặn một lối mà bỏ lối kia thì "khoá" chỉ là hình thức.</li>
 *   <li><b>Không tự hại chính mình</b> — quản trị viên không tự khoá và không tự hạ vai trò, vì hệ
 *       thống chỉ có một cấp quản trị và không còn ai mở lại được.</li>
 *   <li><b>Không lộ thông tin</b> — tổng hợp chi phí AI không được mang theo khoá API hay nội dung
 *       prompt.</li>
 * </ol>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AdminIntegrationTest {

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

    private String adminToken;
    private String adminId;
    private String learnerToken;
    private String creatorToken;

    @BeforeAll
    void setUp() throws Exception {
        // Không tự đăng ký được ADMIN (đăng ký vai trò ADMIN bị hạ xuống LEARNER), nên nâng qua CSDL —
        // đúng như cách quản trị viên đầu tiên của hệ thống thật được tạo
        learnerToken = register("admin-test-learner@example.com", "LEARNER");
        creatorToken = register("admin-test-creator@example.com", "CREATOR");
        register("admin-test-admin@example.com", "LEARNER");
        jdbc.update("update users set role = 'ADMIN' where email = ?", "admin-test-admin@example.com");
        adminToken = login("admin-test-admin@example.com");
        adminId = jdbc.queryForObject("select id::text from users where email = ?",
                String.class, "admin-test-admin@example.com");
    }

    // ============================================================ 1. Phân quyền

    @Test
    @DisplayName("Người học và người tạo nội dung KHÔNG chạm được endpoint quản trị nào")
    void shouldRejectNonAdmin() throws Exception {
        for (String token : new String[]{learnerToken, creatorToken}) {
            mockMvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/api/v1/admin/ai/usage").header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    @DisplayName("Khách chưa đăng nhập nhận 401, không phải 403")
    void shouldRejectGuest() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")).andExpect(status().isUnauthorized());
    }

    // ============================================================ 2. Khoá tài khoản

    @Test
    @DisplayName("Khoá tài khoản: người đó KHÔNG đăng nhập lại được, và nhận thông báo nói rõ lý do")
    void lockedUserCannotLogIn() throws Exception {
        String email = "admin-test-bi-khoa@example.com";
        register(email, "LEARNER");
        String id = idOf(email);

        mockMvc.perform(put("/api/v1/admin/users/{id}/locked", id)
                        .header("Authorization", "Bearer " + adminToken)
                        .param("locked", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locked").value(true));

        // 403 chứ không 401: mật khẩu vẫn đúng, vấn đề là quyền truy cập bị thu hồi. Và thông báo phải
        // nói rõ "bị khoá" — giữ nguyên "email hoặc mật khẩu không đúng" thì người dùng sẽ đi đặt lại
        // mật khẩu hết lần này lần khác mà vẫn không vào được.
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"MatKhau@123"}
                                """.formatted(email)))
                .andExpect(status().isForbidden())
                .andReturn().getResponse().getContentAsString();
        assertThat(body).contains("khoá");
    }

    @Test
    @DisplayName("Khoá tài khoản thu hồi phiên NGAY: refresh token cũ không gia hạn được nữa")
    void lockingRevokesExistingSessions() throws Exception {
        String email = "admin-test-thu-hoi@example.com";
        String refreshToken = registerAndGetRefresh(email);
        String id = idOf(email);

        // Trước khi khoá: refresh chạy bình thường
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(refreshToken)))
                .andExpect(status().isOk());

        String refreshMoi = registerRefreshAgain(email);

        mockMvc.perform(put("/api/v1/admin/users/{id}/locked", id)
                        .header("Authorization", "Bearer " + adminToken)
                        .param("locked", "true"))
                .andExpect(status().isOk());

        // Chỉ đặt cờ mà không thu hồi thì refresh token vẫn gia hạn được tới 14 ngày — tức "khoá" chỉ
        // có hiệu lực sau khi token hết hạn, đúng lúc quản trị viên tin rằng nó có hiệu lực ngay
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(refreshMoi)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Mở khoá thì đăng nhập lại được — khoá là chặn đường vào, không phải xoá người")
    void unlockingRestoresAccess() throws Exception {
        String email = "admin-test-mo-khoa@example.com";
        register(email, "LEARNER");
        String id = idOf(email);

        mockMvc.perform(put("/api/v1/admin/users/{id}/locked", id)
                .header("Authorization", "Bearer " + adminToken).param("locked", "true"));
        mockMvc.perform(put("/api/v1/admin/users/{id}/locked", id)
                        .header("Authorization", "Bearer " + adminToken).param("locked", "false"))
                .andExpect(jsonPath("$.locked").value(false));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"MatKhau@123"}
                                """.formatted(email)))
                .andExpect(status().isOk());
    }

    // ============================================================ 3. Không tự hại chính mình

    @Test
    @DisplayName("Quản trị viên KHÔNG tự khoá được chính mình")
    void adminCannotLockSelf() throws Exception {
        mockMvc.perform(put("/api/v1/admin/users/{id}/locked", adminId)
                        .header("Authorization", "Bearer " + adminToken)
                        .param("locked", "true"))
                .andExpect(status().isBadRequest());

        // Và vẫn vào được sau đó — chốt chặn phải thật sự chặn, không chỉ báo lỗi rồi vẫn ghi
        mockMvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Quản trị viên KHÔNG tự hạ vai trò của chính mình")
    void adminCannotDemoteSelf() throws Exception {
        mockMvc.perform(put("/api/v1/admin/users/{id}/role", adminId)
                        .header("Authorization", "Bearer " + adminToken)
                        .param("role", "LEARNER"))
                .andExpect(status().isBadRequest());

        assertThat(jdbc.queryForObject("select role from users where id = ?::uuid", String.class, adminId))
                .isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("Đổi vai trò người khác thì được, và thu hồi phiên của họ vì vai trò nằm trong token")
    void changeRoleRevokesSessions() throws Exception {
        String email = "admin-test-doi-vai-tro@example.com";
        String refreshToken = registerAndGetRefresh(email);
        String id = idOf(email);

        mockMvc.perform(put("/api/v1/admin/users/{id}/role", id)
                        .header("Authorization", "Bearer " + adminToken)
                        .param("role", "CREATOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CREATOR"));

        // Token cũ mang vai trò cũ; không thu hồi thì người vừa bị hạ quyền vẫn dùng quyền cũ 15 phút
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(refreshToken)))
                .andExpect(status().is4xxClientError());
    }

    // ============================================================ 4. Danh sách và giám sát

    @Test
    @DisplayName("Lọc danh sách theo vai trò và theo trạng thái khoá")
    void shouldFilterUsers() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("role", "CREATOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].role").value(org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.is("CREATOR"))));

        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("keyword", "admin-test-creator"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("admin-test-creator@example.com"));
    }

    @Test
    @DisplayName("Tổng hợp chi phí AI trả về đủ nhóm số liệu và KHÔNG lộ khoá API hay prompt")
    void aiUsageShouldNotLeakSecrets() throws Exception {
        jdbc.update("""
                insert into ai_request_logs (id, user_id, feature, provider, model, tokens_in,
                                            tokens_out, latency_ms, status)
                values (gen_random_uuid(), null, 'chat', 'gemini', 'gemini-3.6-flash', 120, 340, 850, 'SUCCESS')
                """);

        String body = mockMvc.perform(get("/api/v1/admin/ai/usage")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("days", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tongLuotGoi").exists())
                .andExpect(jsonPath("$.tongTokenVao").exists())
                .andExpect(jsonPath("$.theoChucNang").isArray())
                .andExpect(jsonPath("$.theoNhaCungCap").isArray())
                .andReturn().getResponse().getContentAsString();

        assertThat(body.toLowerCase())
                .as("tổng hợp chi phí không được mang theo khoá API hay nội dung prompt")
                .doesNotContain("apikey", "api_key", "x-goog-api-key", "prompt");
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

    private String registerAndGetRefresh(String email) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"MatKhau@123","displayName":"Người dùng","role":"LEARNER"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("refreshToken").asText();
    }

    /** Đăng nhập lại để có refresh token mới (token cũ đã bị rotation thu hồi). */
    private String registerRefreshAgain(String email) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"MatKhau@123"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("refreshToken").asText();
    }

    private String login(String email) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"MatKhau@123"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asText();
    }

    private String idOf(String email) {
        return jdbc.queryForObject("select id::text from users where email = ?", String.class, email);
    }
}
