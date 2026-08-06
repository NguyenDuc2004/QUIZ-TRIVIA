package com.datn.quizai.auth;

import com.datn.quizai.auth.service.PasswordResetOtpService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test luồng quên mật khẩu qua OTP (FR-4) trên PostgreSQL + Redis thật.
 * <p>
 * Không gửi email thật: {@code MailService} chưa cấu hình SMTP thì chỉ ghi log rồi trả false,
 * còn mã OTP vẫn được phát và lưu ở Redis như bình thường. Test đọc thẳng Redis để lấy mã —
 * cách duy nhất kiểm được luồng mà không phụ thuộc hòm thư thật.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PasswordResetFlowIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    private static final String PASSWORD = "MatKhau@123";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private PasswordResetOtpService otpService;

    @BeforeAll
    void register() throws Exception {
        register("reset-flow@example.com");
    }

    @Test
    @DisplayName("Luồng đầy đủ: xin mã → đặt lại mật khẩu → đăng nhập bằng mật khẩu mới")
    void shouldResetPasswordWithOtp() throws Exception {
        String email = "reset-full@example.com";
        register(email);

        requestOtp(email).andExpect(status().isNoContent());
        String code = issueFreshCode(email);

        resetPassword(email, code, "MatKhauMoi@456").andExpect(status().isNoContent());

        login(email, "MatKhauMoi@456").andExpect(status().isOk());
        login(email, PASSWORD).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Email không tồn tại vẫn trả 204 — không để lộ tài khoản nào có trong hệ thống")
    void shouldNotRevealWhetherEmailExists() throws Exception {
        requestOtp("khong-ton-tai-" + System.nanoTime() + "@example.com")
                .andExpect(status().isNoContent());

        // Và không sinh mã nào cho email đó
        assertThat(redisTemplate.hasKey("pwd-otp:khong-ton-tai@example.com")).isFalse();
    }

    @Test
    @DisplayName("Mã OTP lưu ở Redis dưới dạng BĂM, không lưu thô")
    void shouldStoreOtpHashed() throws Exception {
        String email = "reset-hash@example.com";
        register(email);

        String code = issueFreshCode(email);
        String stored = redisTemplate.opsForValue().get("pwd-otp:" + email);

        assertThat(stored).isNotNull().doesNotContain(code);
        // BCrypt bắt đầu bằng $2a$/$2b$
        assertThat(stored).startsWith("$2");
    }

    @Test
    @DisplayName("Mã sai bị từ chối và có đếm số lần còn lại")
    void shouldRejectWrongCode() throws Exception {
        String email = "reset-wrong@example.com";
        register(email);
        issueFreshCode(email);

        resetPassword(email, "000000", "MatKhauMoi@456")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("lần thử")));
    }

    @Test
    @DisplayName("Sai quá số lần cho phép thì mã bị huỷ, mã đúng sau đó cũng vô dụng")
    void shouldBlockAfterTooManyAttempts() throws Exception {
        String email = "reset-bruteforce@example.com";
        register(email);
        String code = issueFreshCode(email);

        // 5 lần sai liên tiếp (app.mail.otp.max-attempts = 5)
        for (int i = 0; i < 5; i++) {
            resetPassword(email, "00000" + i, "MatKhauMoi@456").andExpect(status().isBadRequest());
        }

        // Mã đã bị huỷ nên mã ĐÚNG cũng không dùng được nữa
        resetPassword(email, code, "MatKhauMoi@456").andExpect(status().isBadRequest());
        assertThat(redisTemplate.hasKey("pwd-otp:" + email)).isFalse();
    }

    @Test
    @DisplayName("Mã chỉ dùng được MỘT lần")
    void shouldConsumeOtpAfterUse() throws Exception {
        String email = "reset-once@example.com";
        register(email);
        String code = issueFreshCode(email);

        resetPassword(email, code, "MatKhauMoi@456").andExpect(status().isNoContent());
        resetPassword(email, code, "MatKhauKhac@789").andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Xin mã quá dày bị chặn 429 — không cho bơm email vào hòm thư người khác")
    void shouldRateLimitResend() throws Exception {
        String email = "reset-spam@example.com";
        register(email);

        requestOtp(email).andExpect(status().isNoContent());
        requestOtp(email).andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("Đặt lại mật khẩu thu hồi phiên trên mọi thiết bị")
    void shouldRevokeSessionsAfterReset() throws Exception {
        String email = "reset-sessions@example.com";
        register(email);

        String refreshToken = objectMapper.readTree(
                        login(email, PASSWORD).andReturn().getResponse().getContentAsString())
                .get("refreshToken").asText();

        resetPassword(email, issueFreshCode(email), "MatKhauMoi@456").andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(refreshToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Mật khẩu mới quá ngắn bị chặn 400")
    void shouldValidateNewPassword() throws Exception {
        String email = "reset-short@example.com";
        register(email);

        resetPassword(email, issueFreshCode(email), "123")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.newPassword").exists());
    }

    // ===== Helper =====

    /**
     * Phát mã trực tiếp qua service thay vì gọi API.
     * <p>
     * Gọi API thì vướng giãn cách chống spam giữa các ca test, mà mã thô chỉ tồn tại trong email —
     * Redis chỉ giữ bản băm nên không đọc ngược ra được.
     */
    private String issueFreshCode(String email) {
        redisTemplate.delete("pwd-otp-cooldown:" + email);
        return otpService.issue(email);
    }

    private void register(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s","displayName":"Người dùng test","role":"LEARNER"}
                        """.formatted(email, PASSWORD)));
    }

    private org.springframework.test.web.servlet.ResultActions requestOtp(String email) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\"}".formatted(email)));
    }

    private org.springframework.test.web.servlet.ResultActions resetPassword(
            String email, String otp, String newPassword) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","otp":"%s","newPassword":"%s"}
                        """.formatted(email, otp, newPassword)));
    }

    private org.springframework.test.web.servlet.ResultActions login(String email, String password)
            throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s"}
                        """.formatted(email, password)));
    }
}
