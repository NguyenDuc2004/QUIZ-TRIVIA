package com.datn.quizai.config;

import com.datn.quizai.user.domain.Role;
import com.datn.quizai.user.domain.User;
import com.datn.quizai.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Lớp này cầm quyền tạo tài khoản mạnh nhất hệ thống, nên phần đáng kiểm không phải "có tạo được
 * không" mà là **những lúc nó phải TỪ CHỐI tạo**.
 */
class AdminBootstrapTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        given(passwordEncoder.encode(anyString())).willReturn("da-ma-hoa");
    }

    private AdminBootstrap bootstrap(String email, String matKhau) {
        return new AdminBootstrap(userRepository, passwordEncoder, email, matKhau);
    }

    @Test
    @DisplayName("Đã có admin thì KHÔNG tạo thêm — không dùng được để leo thang về sau")
    void daCoAdminThiBoQua() {
        given(userRepository.countByRole(Role.ADMIN)).willReturn(1L);

        bootstrap("ke-xau@example.com", "MatKhau@123").run(null);

        verify(userRepository, never()).save(any());
        // Không cả tra cứu email: đã có admin thì cấu hình này vô nghĩa, không có nhánh nào chạm tới nó
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("Chưa có admin và có cấu hình đủ thì tạo mới, mật khẩu ĐƯỢC MÃ HOÁ")
    void taoAdminDauTien() {
        given(userRepository.countByRole(Role.ADMIN)).willReturn(0L);
        given(userRepository.findByEmail("admin@quizai.local")).willReturn(Optional.empty());

        bootstrap("admin@quizai.local", "MatKhau@123").run(null);

        ArgumentCaptor<User> daLuu = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(daLuu.capture());
        assertThat(daLuu.getValue().getRole()).isEqualTo(Role.ADMIN);
        assertThat(daLuu.getValue().getEmail()).isEqualTo("admin@quizai.local");
        assertThat(daLuu.getValue().getPasswordHash())
                .as("không bao giờ lưu mật khẩu thô")
                .isEqualTo("da-ma-hoa");
    }

    @Test
    @DisplayName("Không khai gì thì bỏ qua trong hoà bình — chưa dùng tính năng, không phải lỗi")
    void khongKhaiGiThiBoQua() {
        given(userRepository.countByRole(Role.ADMIN)).willReturn(0L);

        bootstrap("", "").run(null);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Khai NỬA VỜI thì dừng hẳn ứng dụng, không im lặng bỏ qua")
    void khaiNuaVoiThiNo() {
        given(userRepository.countByRole(Role.ADMIN)).willReturn(0L);

        // Im lặng bỏ qua mới là lựa chọn tệ: người vận hành nghĩ mình đã cấu hình xong, mà hệ thống
        // vẫn không có admin nào — và họ chỉ phát hiện ra đúng lúc cần đăng nhập vào khu quản trị.
        assertThatThrownBy(() -> bootstrap("admin@quizai.local", "").run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CẢ HAI");

        assertThatThrownBy(() -> bootstrap("", "MatKhau@123").run(null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Mật khẩu quá ngắn thì từ chối — tài khoản quyền cao nhất không được yếu hơn tài khoản người học")
    void matKhauNganThiTuChoi() {
        given(userRepository.countByRole(Role.ADMIN)).willReturn(0L);

        assertThatThrownBy(() -> bootstrap("admin@quizai.local", "1234567").run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("8");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Email đã là tài khoản thường thì NÂNG QUYỀN, không tạo trùng email")
    void nangQuyenTaiKhoanSanCo() {
        // Từ chối cho "an toàn" thì để lại đúng cái bế tắc mà lớp này sinh ra để gỡ: hệ thống không có
        // admin, và cũng không có cách nào tạo. Điều kiện "đúng 0 admin" đã chặn phần nguy hiểm rồi.
        User sanCo = new User("admin@quizai.local", "hash-cu", "Người học", Role.LEARNER);
        given(userRepository.countByRole(Role.ADMIN)).willReturn(0L);
        given(userRepository.findByEmail("admin@quizai.local")).willReturn(Optional.of(sanCo));

        bootstrap("admin@quizai.local", "MatKhau@123").run(null);

        assertThat(sanCo.getRole()).isEqualTo(Role.ADMIN);
        verify(userRepository, never()).save(any());
        assertThat(sanCo.getPasswordHash())
                .as("nâng quyền thôi, KHÔNG đổi mật khẩu của người ta")
                .isEqualTo("hash-cu");
    }
}
