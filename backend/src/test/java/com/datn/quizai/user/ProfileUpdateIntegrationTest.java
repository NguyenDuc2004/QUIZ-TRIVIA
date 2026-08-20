package com.datn.quizai.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cập nhật hồ sơ: tên hiển thị và ảnh đại diện.
 *
 * <h3>Vì sao ảnh đại diện cần chốt an toàn như ảnh bìa quiz</h3>
 * Ảnh đại diện hiện trên <b>thanh điều hướng, bảng xếp hạng, danh sách thành viên lớp và thẻ người chơi
 * trong phòng đấu</b> — tức nó được tải trên màn hình của <i>người khác</i>. Đặt một URL ngoài nghĩa là mỗi
 * người nhìn thấy tên bạn sẽ gửi một request kèm IP tới máy chủ do bạn chọn: theo dõi người dùng khác qua
 * một ô nhập tưởng như vô hại.
 *
 * <h3>Nhưng ảnh Google phải giữ được</h3>
 * Đăng nhập Google lưu ảnh từ CDN của Google — một URL ngoài hợp lệ, do <b>máy chủ</b> ghi lúc đăng nhập.
 * Chặn cứng thì người dùng Google chỉ đổi tên hiển thị thôi cũng bị từ chối, vì form gửi kèm ảnh hiện có.
 * Luật đúng: <b>giữ nguyên thì luôn được, đổi thì phải là ảnh đã tải lên hệ thống này</b>.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProfileUpdateIntegrationTest {

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

    private String token;
    private String email;

    @BeforeAll
    void setUp() throws Exception {
        email = "hoso@example.com";
        String body = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"MatKhau@123","displayName":"Tên cũ","role":"LEARNER"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        token = objectMapper.readTree(body).get("accessToken").asText();
    }

    @Test
    @DisplayName("Đổi được tên hiển thị, và tên mới hiện ngay ở /users/me")
    void shouldUpdateDisplayName() throws Exception {
        capNhat("Nguyễn Văn An", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Nguyễn Văn An"));

        mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(jsonPath("$.displayName").value("Nguyễn Văn An"));
    }

    @Test
    @DisplayName("Tên hiển thị được cắt khoảng trắng thừa")
    void shouldTrimDisplayName() throws Exception {
        capNhat("   Trần Thị Bình   ", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Trần Thị Bình"));
    }

    @Test
    @DisplayName("Tên rỗng bị từ chối — người khác nhìn thấy tên này ở bảng xếp hạng")
    void shouldRejectBlankName() throws Exception {
        capNhat("   ", null).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Nhận ảnh đại diện là đường dẫn nội bộ do hệ thống sinh")
    void shouldAcceptUploadedAvatar() throws Exception {
        capNhat("Có ảnh", "/uploads/images/avatar-abc.png")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value("/uploads/images/avatar-abc.png"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://ke-xau.example/pixel.gif",
            "http://cdn-la.example/anh.png",
            "/uploads/../../etc/passwd",
    })
    @DisplayName("TỪ CHỐI URL ngoài và đường dẫn thoát thư mục")
    void shouldRejectExternalAvatar(String url) throws Exception {
        // Đây là chốt chống theo dõi: ảnh đại diện được tải trên màn hình của NGƯỜI KHÁC
        capNhat("Người dùng", url).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Người đăng nhập Google GIỮ được ảnh Google khi chỉ đổi tên")
    void shouldKeepGoogleAvatarWhenUnchanged() throws Exception {
        String anhGoogle = "https://lh3.googleusercontent.com/a/abc123";
        // Mô phỏng đúng thứ AuthService ghi lúc đăng nhập Google — máy chủ ghi, không phải người dùng gửi
        jdbc.update("update users set avatar_url = ? where email = ?", anhGoogle, email);

        // Form hồ sơ nạp ảnh hiện có rồi gửi lại nguyên vẹn khi người dùng chỉ sửa tên.
        // Chặn cứng URL ngoài thì đúng thao tác này bị từ chối, và người dùng Google không đổi được tên.
        capNhat("Tên mới của người dùng Google", anhGoogle)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value(anhGoogle));
    }

    @Test
    @DisplayName("Nhưng ĐỔI sang một URL ngoài KHÁC thì vẫn bị từ chối")
    void shouldStillRejectSwitchingToAnotherExternalUrl() throws Exception {
        jdbc.update("update users set avatar_url = ? where email = ?",
                "https://lh3.googleusercontent.com/a/abc123", email);

        // Ranh giới của ngoại lệ ở trên: "giữ nguyên thì được" KHÔNG có nghĩa "URL ngoài nào cũng được"
        capNhat("Người dùng", "https://ke-xau.example/pixel.gif")
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Xoá ảnh đại diện bằng cách gửi null")
    void shouldAllowRemovingAvatar() throws Exception {
        capNhat("Không ảnh", "/uploads/images/x.png").andExpect(status().isOk());

        capNhat("Không ảnh", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").doesNotExist());
    }

    @Test
    @DisplayName("Chưa đăng nhập thì không sửa được hồ sơ của ai")
    void shouldRequireAuth() throws Exception {
        mockMvc.perform(put("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Kẻ lạ\"}"))
                .andExpect(status().isUnauthorized());
    }

    private org.springframework.test.web.servlet.ResultActions capNhat(String ten, String anh)
            throws Exception {
        String body = anh == null
                ? "{\"displayName\":%s}".formatted(objectMapper.writeValueAsString(ten))
                : "{\"displayName\":%s,\"avatarUrl\":%s}".formatted(
                        objectMapper.writeValueAsString(ten), objectMapper.writeValueAsString(anh));

        return mockMvc.perform(put("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
