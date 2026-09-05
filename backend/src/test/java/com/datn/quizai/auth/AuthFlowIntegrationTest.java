package com.datn.quizai.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test end-to-end lát cắt Auth trên PostgreSQL + Redis thật (Testcontainers):
 * đăng ký → dùng token gọi /users/me → refresh có rotation → đăng xuất.
 * Đồng thời kiểm luật Guest của {@link com.datn.quizai.config.SecurityConfig}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AuthFlowIntegrationTest {

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

    @Test
    @DisplayName("Luồng đầy đủ: đăng ký → /users/me bằng token → refresh (rotation) → đăng xuất")
    void shouldCompleteFullAuthFlow() throws Exception {
        // 1. Đăng ký
        String registerBody = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"e2e@example.com","password":"MatKhau@123",
                                 "displayName":"Người Dùng E2E","role":"CREATOR"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode registered = objectMapper.readTree(registerBody);
        String accessToken = registered.get("accessToken").asText();
        String refreshToken = registered.get("refreshToken").asText();

        // 2. Gọi endpoint cần đăng nhập bằng access token
        mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("e2e@example.com"))
                .andExpect(jsonPath("$.displayName").value("Người Dùng E2E"))
                .andExpect(jsonPath("$.role").value("CREATOR"));

        // 3. Refresh: nhận refresh token mới
        String refreshBody = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshBody(refreshToken))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String rotatedToken = objectMapper.readTree(refreshBody).get("refreshToken").asText();
        assertThat(rotatedToken).isNotEqualTo(refreshToken);

        // 4. Refresh token cũ đã bị thu hồi → 401
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshBody(refreshToken))))
                .andExpect(status().isUnauthorized());

        // 5. Đăng xuất rồi refresh lại → 401
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshBody(rotatedToken))))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshBody(rotatedToken))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Guest chưa đăng nhập: /users/me → 401 với response lỗi chuẩn")
    void shouldRejectGuestOnProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Bạn cần đăng nhập để sử dụng chức năng này"))
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    @DisplayName("Token sai chữ ký / rác → 401")
    void shouldRejectGarbageToken() throws Exception {
        mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer abc.def.ghi"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Guest gọi endpoint công khai (tài liệu API) → 200")
    void shouldAllowGuestOnPublicEndpoint() throws Exception {
        // Không dùng /actuator/health ở đây để test không phụ thuộc Neo4j (Auth chưa cần Neo4j)
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Đường dẫn không tồn tại → 404, KHÔNG phải 500")
    void duongDanKhongTonTaiTra404() throws Exception {
        // Trước khi sửa: 500 "Đã có lỗi xảy ra" kèm stack trace ghi ở mức ERROR.
        //
        // `GlobalExceptionHandler` CÓ sẵn một nhánh bắt `NoHandlerFoundException`, nhưng đó là nhánh
        // CHẾT: Spring Boot 3 chỉ ném nó khi bật `spring.mvc.throw-exception-if-no-handler-found`, mà
        // dự án không bật. Thực tế request rơi xuống bộ xử lý tài nguyên tĩnh và nhận
        // `NoResourceFoundException` — một kiểu khác hẳn, không kế thừa từ kiểu kia.
        //
        // Hai cái giá của 500: client không phân biệt được "gõ sai địa chỉ" với "server hỏng", và log
        // đầy stack trace của những đường dẫn gõ sai, làm loãng đúng thứ mức ERROR sinh ra để đánh dấu.
        // Gọi KÈM token. Khách vãng lai gõ sai đường dẫn thì nhận 401, và đó là đúng: chưa đăng nhập
        // thì không được biết đường dẫn nào có tồn tại. 404 chỉ dành cho người đã qua cửa xác thực.
        String token = objectMapper.readTree(mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"go-sai-duong-dan@example.com","password":"MatKhau@123",
                                 "displayName":"Người dùng"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString())
                .get("accessToken").asText();

        mockMvc.perform(get("/api/v1/auth/khong-he-ton-tai")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Người học tự lên CREATOR được, và nhận token MỚI mang vai trò mới")
    void tuLenCreator() throws Exception {
        String token = dangKy("len-creator@example.com", "LEARNER");

        String body = mockMvc.perform(patch("/api/v1/auth/my-role")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"CREATOR\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("CREATOR"))
                .andReturn().getResponse().getContentAsString();

        // Token MỚI phải dùng được ngay. Không cấp lại token thì vai trò cũ còn nằm trong access token
        // đang cầm tới 15 phút — người vừa lên Creator bấm vào menu mới và nhận 403.
        String tokenMoi = objectMapper.readTree(body).get("accessToken").asText();
        mockMvc.perform(get("/api/v1/ai/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenMoi))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Không tự cấp ADMIN cho mình bằng đường đổi vai trò")
    void khongTuLenAdmin() throws Exception {
        // Đây là chốt chặn quan trọng nhất của endpoint này: nếu thủng thì mọi tài khoản đều thành
        // quản trị được, trong khi cả `register` lẫn đường Google đều đã chặn cẩn thận.
        String token = dangKy("doi-lam-admin@example.com", "LEARNER");

        mockMvc.perform(patch("/api/v1/auth/my-role")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Khách chưa đăng nhập không đổi được vai trò của ai")
    void khachKhongDoiDuocVaiTro() throws Exception {
        mockMvc.perform(patch("/api/v1/auth/my-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"CREATOR\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Creator chuyển ngược về Người học được — đường một chiều thì người ta ngại bấm")
    void quayVeNguoiHoc() throws Exception {
        String token = dangKy("ve-nguoi-hoc@example.com", "CREATOR");

        mockMvc.perform(patch("/api/v1/auth/my-role")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"LEARNER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("LEARNER"));
    }

    /** Đăng ký nhanh, trả access token. */
    private String dangKy(String email, String role) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"MatKhau@123","displayName":"Người dùng",
                                 "role":"%s"}
                                """.formatted(email, role)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asText();
    }

    @Test
    @DisplayName("Tự đăng ký ADMIN → hệ thống hạ xuống LEARNER")
    void shouldNotAllowSelfServiceAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"xin-admin@example.com","password":"MatKhau@123",
                                 "displayName":"Xin Admin","role":"ADMIN"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("LEARNER"));
    }

    private record RefreshBody(String refreshToken) {
    }
}
