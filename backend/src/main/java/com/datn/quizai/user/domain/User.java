package com.datn.quizai.user.domain;

import com.datn.quizai.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Người dùng hệ thống — bảng `users` (docs/database.md §1.2). */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User extends BaseEntity {

    /** Luôn lưu chữ thường (chuẩn hóa ở service) để so sánh không phân biệt hoa/thường. */
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    /** NULL = tài khoản chỉ đăng nhập bằng Google, chưa từng đặt mật khẩu. */
    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    /**
     * Google subject id ({@code sub}) — định danh ổn định, không đổi kể cả khi người dùng đổi địa
     * chỉ Gmail. Vì vậy đây mới là khoá liên kết tài khoản Google, không phải email.
     */
    @Column(name = "google_id", length = 64)
    private String googleId;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.LEARNER;

    /**
     * Quản trị viên đã khoá tài khoản này hay chưa (V12).
     * <p>
     * Khoá là <b>chặn đường vào</b>, không phải xoá người dùng: bài đã làm, quiz đã soạn và học liệu
     * đã nạp đều là dữ liệu người khác đang dùng hoặc đang được thống kê.
     */
    @Column(nullable = false)
    private boolean locked = false;

    /**
     * Hạn mức số lượt gọi AI mỗi ngày của riêng người này (features/10, FR-84).
     * <p>
     * <b>null KHÁC 0.</b> {@code null} = chưa đặt riêng, dùng mặc định hệ thống; {@code 0} = quản trị viên
     * cấm người này gọi AI. Gộp hai thứ lại thì hoặc không cấm được ai, hoặc mọi tài khoản mới bị cấm ngay
     * từ lúc tạo — nên kiểu bao {@code Integer}, không phải {@code int}.
     */
    @Column(name = "ai_daily_quota")
    private Integer aiDailyQuota;

    public User(String email, String passwordHash, String displayName, Role role) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.role = role;
    }

    /** Đăng nhập được bằng email + mật khẩu hay chỉ bằng Google. */
    public boolean hasPassword() {
        return passwordHash != null && !passwordHash.isBlank();
    }
}
