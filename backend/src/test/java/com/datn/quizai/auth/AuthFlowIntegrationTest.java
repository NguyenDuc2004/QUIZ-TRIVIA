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
