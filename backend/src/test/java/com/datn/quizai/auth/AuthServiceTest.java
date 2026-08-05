package com.datn.quizai.auth;

import com.datn.quizai.auth.dto.AuthResponse;
import com.datn.quizai.auth.dto.ChangePasswordRequest;
import com.datn.quizai.auth.dto.LoginRequest;
import com.datn.quizai.auth.dto.RegisterRequest;
import com.datn.quizai.common.exception.BusinessException;
import com.datn.quizai.user.Role;
import com.datn.quizai.user.User;
import com.datn.quizai.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/** Unit test logic xác thực — docs/features/01-auth.md. */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService authService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    // ===== Đăng ký =====

    @Test
    @DisplayName("Đăng ký thành công: hạ email về chữ thường, băm mật khẩu, trả cặp token")
    void shouldRegisterAndReturnTokens() {
        given(userRepository.existsByEmail("minh.duc@example.com")).willReturn(false);
        given(passwordEncoder.encode("MatKhau@123")).willReturn("hash-bcrypt");
        given(userRepository.save(any(User.class))).willAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(USER_ID);
            return saved;
        });
        given(jwtService.generateAccessToken(any(User.class))).willReturn("access-token");
        given(jwtService.accessTtlSeconds()).willReturn(900L);
        given(refreshTokenService.issue(USER_ID)).willReturn("refresh-token");

        AuthResponse response = authService.register(new RegisterRequest(
                "  Minh.Duc@Example.COM ", "MatKhau@123", "  Nguyễn Khắc Minh Đức  ", Role.CREATOR));

        then(userRepository).should().save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(saved.getEmail()).isEqualTo("minh.duc@example.com");
        assertThat(saved.getPasswordHash()).isEqualTo("hash-bcrypt");
        assertThat(saved.getDisplayName()).isEqualTo("Nguyễn Khắc Minh Đức");
        assertThat(saved.getRole()).isEqualTo(Role.CREATOR);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(900L);
        assertThat(response.user().email()).isEqualTo("minh.duc@example.com");
    }

    @Test
    @DisplayName("Email đã tồn tại (khác hoa/thường) → 409, không lưu gì")
    void shouldReturn409WhenEmailExists() {
        given(userRepository.existsByEmail("minh.duc@example.com")).willReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest(
                "MINH.DUC@EXAMPLE.COM", "MatKhau@123", "Trùng", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Email này đã được sử dụng")
                .extracting(ex -> ((BusinessException) ex).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        then(userRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("Tự đăng ký vai trò ADMIN → bị hạ xuống LEARNER (chống leo thang quyền)")
    void shouldDowngradeSelfAssignedAdminRole() {
        stubSuccessfulSave();

        authService.register(new RegisterRequest("a@example.com", "MatKhau@123", "Xin Admin", Role.ADMIN));

        then(userRepository).should().save(userCaptor.capture());
        assertThat(userCaptor.getValue().getRole()).isEqualTo(Role.LEARNER);
    }

    @Test
    @DisplayName("Không chọn vai trò → mặc định LEARNER")
    void shouldDefaultToLearnerRole() {
        stubSuccessfulSave();

        authService.register(new RegisterRequest("b@example.com", "MatKhau@123", "Không chọn", null));

        then(userRepository).should().save(userCaptor.capture());
        assertThat(userCaptor.getValue().getRole()).isEqualTo(Role.LEARNER);
    }

    // ===== Đăng nhập =====

    @Test
    @DisplayName("Đăng nhập đúng mật khẩu → trả token")
    void shouldLoginSuccessfully() {
        User user = existingUser("hash-bcrypt");
        given(userRepository.findByEmail("minh.duc@example.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("MatKhau@123", "hash-bcrypt")).willReturn(true);
        given(jwtService.generateAccessToken(user)).willReturn("access-token");
        given(refreshTokenService.issue(USER_ID)).willReturn("refresh-token");

        AuthResponse response = authService.login(new LoginRequest("Minh.Duc@Example.COM", "MatKhau@123"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.user().displayName()).isEqualTo("Nguyễn Khắc Minh Đức");
    }

    @Test
    @DisplayName("Sai mật khẩu → 401 và không phát refresh token")
    void shouldReject401WhenPasswordWrong() {
        given(userRepository.findByEmail("minh.duc@example.com"))
                .willReturn(Optional.of(existingUser("hash-bcrypt")));
        given(passwordEncoder.matches("SaiRoi", "hash-bcrypt")).willReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("minh.duc@example.com", "SaiRoi")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Email hoặc mật khẩu không đúng");

        then(refreshTokenService).should(never()).issue(any());
    }

    @Test
    @DisplayName("Email không tồn tại → 401 với CÙNG thông báo như sai mật khẩu (không tiết lộ email nào có thật)")
    void shouldNotRevealWhetherEmailExists() {
        given(userRepository.findByEmail("khongco@example.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("khongco@example.com", "MatKhau@123")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Email hoặc mật khẩu không đúng");
    }

    // ===== Làm mới token =====

    @Test
    @DisplayName("Refresh → thu hồi token cũ, phát token mới (rotation)")
    void shouldRotateRefreshToken() {
        User user = existingUser("hash-bcrypt");
        given(refreshTokenService.resolve("cu")).willReturn(USER_ID);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(refreshTokenService.rotate("cu")).willReturn("moi");
        given(jwtService.generateAccessToken(user)).willReturn("access-token-moi");

        AuthResponse response = authService.refresh("cu");

        assertThat(response.refreshToken()).isEqualTo("moi");
        then(refreshTokenService).should().rotate("cu");
    }

    @Test
    @DisplayName("Refresh khi tài khoản đã bị xóa → 401")
    void shouldReject401WhenUserGoneOnRefresh() {
        given(refreshTokenService.resolve("cu")).willReturn(USER_ID);
        given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("cu"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Tài khoản không còn tồn tại");
    }

    @Test
    @DisplayName("Đăng xuất → thu hồi refresh token")
    void shouldRevokeTokenOnLogout() {
        authService.logout("token-can-thu-hoi");

        then(refreshTokenService).should().revoke("token-can-thu-hoi");
    }

    // ===== Đổi mật khẩu =====

    @Test
    @DisplayName("Đổi mật khẩu thành công → lưu hash mới")
    void shouldChangePassword() {
        User user = existingUser("hash-cu");
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(passwordEncoder.matches("MatKhauCu@1", "hash-cu")).willReturn(true);
        given(passwordEncoder.matches("MatKhauMoi@2", "hash-cu")).willReturn(false);
        given(passwordEncoder.encode("MatKhauMoi@2")).willReturn("hash-moi");

        authService.changePassword(USER_ID, new ChangePasswordRequest("MatKhauCu@1", "MatKhauMoi@2"));

        assertThat(user.getPasswordHash()).isEqualTo("hash-moi");
    }

    @Test
    @DisplayName("Sai mật khẩu hiện tại → 401, không đổi gì")
    void shouldReject401WhenCurrentPasswordWrong() {
        User user = existingUser("hash-cu");
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(passwordEncoder.matches("SaiRoi", "hash-cu")).willReturn(false);

        assertThatThrownBy(() -> authService.changePassword(USER_ID,
                new ChangePasswordRequest("SaiRoi", "MatKhauMoi@2")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Mật khẩu hiện tại không đúng");

        assertThat(user.getPasswordHash()).isEqualTo("hash-cu");
    }

    @Test
    @DisplayName("Mật khẩu mới trùng mật khẩu cũ → 400")
    void shouldReject400WhenNewPasswordSameAsOld() {
        User user = existingUser("hash-cu");
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(passwordEncoder.matches("MatKhauCu@1", "hash-cu")).willReturn(true);

        assertThatThrownBy(() -> authService.changePassword(USER_ID,
                new ChangePasswordRequest("MatKhauCu@1", "MatKhauCu@1")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Mật khẩu mới phải khác mật khẩu hiện tại");
    }

    // ===== Helper =====

    private User existingUser(String passwordHash) {
        User user = new User("minh.duc@example.com", passwordHash, "Nguyễn Khắc Minh Đức", Role.CREATOR);
        user.setId(USER_ID);
        return user;
    }

    private void stubSuccessfulSave() {
        given(userRepository.existsByEmail(any())).willReturn(false);
        given(passwordEncoder.encode(any())).willReturn("hash-bcrypt");
        given(userRepository.save(any(User.class))).willAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(USER_ID);
            return saved;
        });
        given(refreshTokenService.issue(USER_ID)).willReturn("refresh-token");
    }
}
