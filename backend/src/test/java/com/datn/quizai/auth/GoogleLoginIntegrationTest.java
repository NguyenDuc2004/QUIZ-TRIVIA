package com.datn.quizai.auth;

import com.datn.quizai.auth.service.GoogleTokenVerifier;
import com.datn.quizai.user.domain.Role;
import com.datn.quizai.user.domain.User;
import com.datn.quizai.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test đăng nhập bằng Google (FR-3).
 * <p>
 * {@link GoogleTokenVerifier} được thay bằng mock: nó gọi ra máy chủ Google thật để lấy bộ khoá
 * công khai, mà test không được phụ thuộc mạng ngoài — và cũng không có cách nào tạo ID token
 * hợp lệ do Google ký. Phần <i>xác minh chữ ký</i> tin vào thư viện chính chủ; phần test ở đây lo
 * <b>nghiệp vụ sau khi đã xác minh</b>: liên kết tài khoản, tạo mới, và không tự phong vai trò.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GoogleLoginIntegrationTest {

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
    private UserRepository userRepository;

    @MockitoBean
    private GoogleTokenVerifier googleTokenVerifier;

    private void googleReturns(String subject, String email, String name) {
        given(googleTokenVerifier.verify(anyString()))
                .willReturn(new GoogleTokenVerifier.GoogleAccount(
                        subject, email, true, name, "https://lh3.googleusercontent.com/anh.jpg"));
    }

    @Test
    @DisplayName("Người dùng hoàn toàn mới: tạo tài khoản không mật khẩu, vai trò LEARNER")
    void shouldCreateAccountForNewGoogleUser() throws Exception {
        String email = "google-moi-" + UUID.randomUUID() + "@example.com";
        googleReturns("sub-" + UUID.randomUUID(), email, "Người Mới");

        loginWithGoogle()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.user.displayName").value("Người Mới"))
                // Không gửi vai trò thì mặc định LEARNER, y như đăng ký thường bỏ trống trường đó
                .andExpect(jsonPath("$.user.role").value("LEARNER"));

        User created = userRepository.findByEmail(email).orElseThrow();
        assertThat(created.hasPassword()).isFalse();
        assertThat(created.getGoogleId()).isNotBlank();
        assertThat(created.getAvatarUrl()).contains("googleusercontent.com");
    }

    @Test
    @DisplayName("Email đã có tài khoản mật khẩu: LIÊN KẾT vào đó, không tạo tài khoản thứ hai")
    void shouldLinkToExistingAccount() throws Exception {
        String email = "google-lienket-" + UUID.randomUUID() + "@example.com";
        register(email);
        UUID existingId = userRepository.findByEmail(email).orElseThrow().getId();

        googleReturns("sub-" + UUID.randomUUID(), email, "Tên Từ Google");
        loginWithGoogle().andExpect(status().isOk())
                .andExpect(jsonPath("$.user.id").value(existingId.toString()));

        User linked = userRepository.findById(existingId).orElseThrow();
        assertThat(linked.getGoogleId()).isNotBlank();
        // Vẫn giữ mật khẩu cũ: người dùng đăng nhập được bằng cả hai cách
        assertThat(linked.hasPassword()).isTrue();
        // Không đè tên người dùng đã tự đặt
        assertThat(linked.getDisplayName()).isEqualTo("Người dùng test");
    }

    @Test
    @DisplayName("Đăng nhập Google lần hai khớp theo google_id, không tạo trùng")
    void shouldReuseAccountOnSecondLogin() throws Exception {
        String email = "google-lan2-" + UUID.randomUUID() + "@example.com";
        String subject = "sub-" + UUID.randomUUID();

        googleReturns(subject, email, "Người Dùng");
        loginWithGoogle().andExpect(status().isOk());
        long afterFirst = userRepository.count();

        loginWithGoogle().andExpect(status().isOk());
        assertThat(userRepository.count()).isEqualTo(afterFirst);
    }

    @Test
    @DisplayName("Token Google không hợp lệ trả 401")
    void shouldRejectInvalidToken() throws Exception {
        willThrow(com.datn.quizai.common.exception.BusinessException.unauthorized("Token Google không hợp lệ"))
                .given(googleTokenVerifier).verify(anyString());

        loginWithGoogle().andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Thiếu idToken trả 400")
    void shouldValidateRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.idToken").exists());
    }

    @Test
    @DisplayName("Tài khoản chỉ-Google đổi mật khẩu → 400 kèm hướng dẫn, không phải lỗi khó hiểu")
    void shouldExplainWhenGoogleOnlyAccountChangesPassword() throws Exception {
        String email = "google-nopass-" + UUID.randomUUID() + "@example.com";
        googleReturns("sub-" + UUID.randomUUID(), email, "Không Mật Khẩu");

        String body = loginWithGoogle().andReturn().getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(body).get("accessToken").asText();

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"khong-co","newPassword":"MatKhauMoi@456"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("Quên mật khẩu")));
    }

    // ===== Helper =====

    // ============================================================ vai trò khi đăng ký bằng Google

    @Test
    @DisplayName("Tài khoản MỚI: nhận đúng vai trò CREATOR người dùng chọn ở trang đăng ký")
    void shouldHonorRoleForNewAccount() throws Exception {
        String email = "google-creator-" + UUID.randomUUID() + "@example.com";
        googleReturns("sub-" + UUID.randomUUID(), email, "Người Tạo");

        // Trước đây vai trò bị đặt cứng LEARNER, nên người dùng chọn "Tạo quiz, sinh đề AI" rồi bấm
        // Google sẽ bị bỏ qua lựa chọn TRONG IM LẶNG — vào hệ thống mà không thấy mục soạn quiz đâu.
        loginWithGoogle("CREATOR")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("CREATOR"));

        assertThat(userRepository.findByEmail(email).orElseThrow().getRole())
                .isEqualTo(Role.CREATOR);
    }

    @Test
    @DisplayName("Tài khoản MỚI xin ADMIN: bị hạ xuống LEARNER — ranh giới an ninh thật nằm ở đây")
    void shouldDowngradeAdminForNewAccount() throws Exception {
        String email = "google-admin-" + UUID.randomUUID() + "@example.com";
        googleReturns("sub-" + UUID.randomUUID(), email, "Kẻ Xin Quyền");

        // Cùng luật với đăng ký thường (docs/security.md §1): ADMIN chỉ được cấp bởi Admin sẵn có.
        // CREATOR thì không phải ranh giới — đăng ký bằng email vốn đã cho tự chọn.
        loginWithGoogle("ADMIN")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("LEARNER"));
    }

    @Test
    @DisplayName("ĐÃ CÓ tài khoản LEARNER: đăng nhập lại kèm role=CREATOR KHÔNG được nâng cấp")
    void mustNotUpgradeExistingAccountOnLogin() throws Exception {
        String email = "google-nangcap-" + UUID.randomUUID() + "@example.com";
        String sub = "sub-" + UUID.randomUUID();

        // Lần đầu: tạo tài khoản LEARNER
        googleReturns(sub, email, "Người Học");
        loginWithGoogle().andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("LEARNER"));

        // Lần hai: CÙNG tài khoản đó, nhưng gửi kèm CREATOR.
        //
        // Đây là ranh giới quan trọng nhất của cả thay đổi này. Endpoint /auth/google dùng chung cho
        // ĐĂNG NHẬP lẫn ĐĂNG KÝ — Google không phân biệt hai việc đó. Nếu áp vai trò ở mọi lần gọi thì
        // bất kỳ ai cũng tự lên CREATOR bằng cách đăng nhập lại một lần nữa.
        loginWithGoogle("CREATOR")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("LEARNER"));

        assertThat(userRepository.findByEmail(email).orElseThrow().getRole())
                .as("tài khoản đã tồn tại phải giữ nguyên vai trò")
                .isEqualTo(Role.LEARNER);
    }

    @Test
    @DisplayName("LIÊN KẾT vào tài khoản email sẵn có: giữ nguyên vai trò, bỏ qua role gửi kèm")
    void mustNotUpgradeWhenLinkingExistingEmailAccount() throws Exception {
        String email = "google-lienket-vaitro-" + UUID.randomUUID() + "@example.com";
        register(email);   // tạo tài khoản LEARNER bằng email + mật khẩu

        googleReturns("sub-" + UUID.randomUUID(), email, "Tên Từ Google");

        // Liên kết KHÔNG phải tạo mới. Áp vai trò ở đây mở đúng đường tự nâng cấp mà thiết kế này tránh:
        // một người dùng LEARNER chỉ cần liên kết Google là lên CREATOR.
        loginWithGoogle("CREATOR")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("LEARNER"));
    }

    private org.springframework.test.web.servlet.ResultActions loginWithGoogle() throws Exception {
        return loginWithGoogle(null);
    }

    /** @param role vai trò gửi kèm; null = không gửi trường đó, đúng như trang ĐĂNG NHẬP làm */
    private org.springframework.test.web.servlet.ResultActions loginWithGoogle(String role) throws Exception {
        String body = role == null
                ? "{\"idToken\":\"token-gia-lap\"}"
                : "{\"idToken\":\"token-gia-lap\",\"role\":\"%s\"}".formatted(role);

        return mockMvc.perform(post("/api/v1/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private void register(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"MatKhau@123","displayName":"Người dùng test","role":"LEARNER"}
                                """.formatted(email)))
                .andExpect(status().isCreated());
    }
}
