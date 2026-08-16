package com.datn.quizai.admin.dto;

import com.datn.quizai.user.domain.Role;
import com.datn.quizai.user.domain.User;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Một người dùng trong danh sách quản trị (features/10).
 * <p>
 * Khác {@code UserResponse} dùng cho chính chủ tài khoản ở hai điểm, và cả hai đều có lý do:
 * <ul>
 *   <li>Có thêm {@code locked} và {@code createdAt} — quản trị viên cần thấy trạng thái và thời điểm
 *       tạo để quyết định xử lý.</li>
 *   <li><b>Không</b> có bất cứ thứ gì liên quan tới mật khẩu, kể cả cờ "có mật khẩu hay không". Quản
 *       trị viên không cần biết người dùng đăng nhập bằng cách nào để khoá hay đổi vai trò họ, nên
 *       không đưa ra.</li>
 * </ul>
 *
 * @param loginMethod cách đăng nhập, dạng chữ mô tả (mật khẩu / Google / cả hai) — đủ để hỗ trợ người
 *                    dùng khi họ báo không vào được, mà không phơi trạng thái mật khẩu ra API
 */
public record AdminUserResponse(
        UUID id,
        String email,
        String displayName,
        String avatarUrl,
        Role role,
        boolean locked,
        String loginMethod,
        OffsetDateTime createdAt
) {
    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getRole(),
                user.isLocked(),
                loginMethodOf(user),
                user.getCreatedAt());
    }

    private static String loginMethodOf(User user) {
        boolean matKhau = user.hasPassword();
        boolean google = user.getGoogleId() != null;
        if (matKhau && google) {
            return "Mật khẩu và Google";
        }
        return google ? "Google" : "Mật khẩu";
    }
}
