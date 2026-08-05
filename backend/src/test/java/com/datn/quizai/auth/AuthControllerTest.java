package com.datn.quizai.auth;

import com.datn.quizai.auth.dto.AuthResponse;
import com.datn.quizai.common.exception.BusinessException;
import com.datn.quizai.common.exception.GlobalExceptionHandler;
import com.datn.quizai.user.Role;
import com.datn.quizai.user.dto.UserResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test tầng web của {@link AuthController}: validation, mã trạng thái và
 * response lỗi chuẩn từ {@link GlobalExceptionHandler} (docs/api.md §10).
 * <p>
 * Tắt security filter ở đây để test riêng phần MVC — luật phân quyền được kiểm
 * trong {@link AuthFlowIntegrationTest}.
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    /**
     * {@code @WebMvcTest} vẫn nạp các bean {@link jakarta.servlet.Filter}, nên
     * {@link JwtAuthenticationFilter} cần {@link JwtService} để khởi tạo được
     * (filter không chạy vì đã tắt qua addFilters = false).
     */
    @MockitoBean
    private JwtService jwtService;

    @Test
    @DisplayName("POST /auth/register hợp lệ → 201 kèm cặp token")
    void shouldReturn201OnValidRegister() throws Exception {
        given(authService.register(any())).willReturn(sampleAuthResponse());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"minh.duc@example.com","password":"MatKhau@123",
                                 "displayName":"Nguyễn Khắc Minh Đức","role":"CREATOR"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("minh.duc@example.com"))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("Email sai định dạng, mật khẩu quá ngắn, tên rỗng → 400 kèm fieldErrors tiếng Việt")
    void shouldReturn400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"khong-phai-email","password":"123","displayName":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/register"))
                .andExpect(jsonPath("$.traceId").exists())
                .andExpect(jsonPath("$.fieldErrors.email").value("Email không đúng định dạng"))
                .andExpect(jsonPath("$.fieldErrors.password").value("Mật khẩu phải từ 8 đến 72 ký tự"))
                .andExpect(jsonPath("$.fieldErrors.displayName").value("Tên hiển thị không được để trống"));
    }

    @Test
    @DisplayName("Email đã tồn tại → 409 theo response lỗi chuẩn")
    void shouldReturn409WhenEmailExists() throws Exception {
        willThrow(BusinessException.conflict("Email này đã được sử dụng"))
                .given(authService).register(any());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"trung@example.com","password":"MatKhau@123","displayName":"Trùng"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Email này đã được sử dụng"));
    }

    @Test
    @DisplayName("Đăng nhập sai thông tin → 401, thông báo không tiết lộ email có tồn tại hay không")
    void shouldReturn401OnBadLogin() throws Exception {
        willThrow(BusinessException.unauthorized("Email hoặc mật khẩu không đúng"))
                .given(authService).login(any());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"minh.duc@example.com","password":"SaiRoi"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Email hoặc mật khẩu không đúng"));
    }

    @Test
    @DisplayName("JSON sai cú pháp → 400 (không phải 500)")
    void shouldReturn400OnMalformedJson() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": broken"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("Thiếu refreshToken khi logout → 400")
    void shouldReturn400WhenRefreshTokenMissing() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.refreshToken")
                        .value("Refresh token không được để trống"));
    }

    private AuthResponse sampleAuthResponse() {
        UserResponse user = new UserResponse(
                UUID.randomUUID(), "minh.duc@example.com", "Nguyễn Khắc Minh Đức",
                null, Role.CREATOR, OffsetDateTime.now());
        return AuthResponse.of("access-token", "refresh-token", 900L, user);
    }
}
