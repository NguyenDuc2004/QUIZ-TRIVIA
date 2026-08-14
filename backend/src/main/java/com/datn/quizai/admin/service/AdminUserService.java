package com.datn.quizai.admin.service;

import com.datn.quizai.admin.dto.AdminUserResponse;
import com.datn.quizai.auth.service.RefreshTokenService;
import com.datn.quizai.common.dto.PageResponse;
import com.datn.quizai.common.exception.BusinessException;
import com.datn.quizai.user.domain.Role;
import com.datn.quizai.user.domain.User;
import com.datn.quizai.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Quản lý người dùng cho quản trị viên (features/10).
 * <p>
 * <b>Hai điều quản trị viên KHÔNG làm được, và đó là chủ ý:</b>
 * <ul>
 *   <li><b>Không tự khoá hoặc tự hạ vai trò chính mình.</b> Hệ thống chỉ có một cấp quản trị, nên một
 *       lần bấm sai là mất quyền quản trị mà không còn ai mở lại được — trừ khi sửa trực tiếp cơ sở dữ
 *       liệu. Chặn ở tầng nghiệp vụ thay vì tin vào việc giao diện ẩn nút.</li>
 *   <li><b>Không xoá người dùng.</b> Bài đã làm, quiz đã soạn, học liệu đã nạp đều là dữ liệu người
 *       khác đang dùng hoặc đang được thống kê; xoá tài khoản kéo theo xoá hoặc làm mồ côi những thứ
 *       đó. Biện pháp tương ứng là <i>khoá</i>: chặn đường vào, giữ nguyên dữ liệu.</li>
 * </ul>
 */
@Service
public class AdminUserService {

    private static final Logger log = LoggerFactory.getLogger(AdminUserService.class);

    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;

    public AdminUserService(UserRepository userRepository, RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> search(String keyword, Role role, Boolean locked,
                                                  Pageable pageable) {
        // Bọc `%` và hạ chữ thường ở ĐÂY, không trong JPQL: truy vấn chỉ được gọi lower() lên cột.
        // Gọi lower() lên tham số thì lúc keyword = null, PostgreSQL nhận kiểu bytea và đổ toàn bộ
        // truy vấn — xem ghi chú ở UserRepository#search.
        String kw = keyword == null || keyword.isBlank()
                ? null
                : "%" + keyword.trim().toLowerCase() + "%";
        return PageResponse.of(userRepository.search(kw, role, locked, pageable),
                AdminUserResponse::from);
    }

    /**
     * Đổi vai trò của một người dùng.
     * <p>
     * Không có bước "xác nhận" nào ở tầng này ngoài việc chặn tự hạ quyền chính mình: đổi vai trò là
     * thao tác đảo lại được, khác với xoá.
     */
    @Transactional
    public AdminUserResponse changeRole(UUID targetId, Role newRole, UUID currentAdminId) {
        User user = require(targetId);

        if (targetId.equals(currentAdminId) && newRole != Role.ADMIN) {
            throw BusinessException.badRequest(
                    "Không thể tự hạ vai trò của chính mình — sẽ không còn ai mở lại được quyền quản trị.");
        }

        Role old = user.getRole();
        if (old == newRole) {
            return AdminUserResponse.from(user);
        }
        user.setRole(newRole);

        // Vai trò nằm TRONG access token, nên token đang lưu ở máy người dùng vẫn mang vai trò cũ tới
        // khi hết hạn (15 phút). Thu hồi phiên để lần gọi API kế tiếp buộc phải lấy token mới — nếu
        // không, người vừa bị hạ quyền vẫn dùng được quyền cũ trong khoảng đó.
        int revoked = refreshTokenService.revokeAll(targetId);
        log.info("Quản trị {} đổi vai trò của {}: {} -> {}, thu hồi {} phiên",
                currentAdminId, targetId, old, newRole, revoked);
        return AdminUserResponse.from(user);
    }

    /**
     * Khoá hoặc mở khoá một tài khoản.
     * <p>
     * Khoá xong phải <b>thu hồi mọi phiên</b> của người đó. Chỉ đặt cờ mà không thu hồi thì access
     * token đang cầm vẫn dùng được tới lúc hết hạn, và refresh token vẫn gia hạn được — tức "khoá" chỉ
     * có hiệu lực sau vài phút, đúng lúc quản trị viên tin rằng nó có hiệu lực ngay.
     */
    @Transactional
    public AdminUserResponse setLocked(UUID targetId, boolean locked, UUID currentAdminId) {
        User user = require(targetId);

        if (targetId.equals(currentAdminId) && locked) {
            throw BusinessException.badRequest(
                    "Không thể tự khoá tài khoản của chính mình — sẽ không còn ai mở lại được.");
        }

        if (user.isLocked() == locked) {
            return AdminUserResponse.from(user);
        }
        user.setLocked(locked);

        if (locked) {
            int revoked = refreshTokenService.revokeAll(targetId);
            log.info("Quản trị {} KHOÁ tài khoản {}, thu hồi {} phiên", currentAdminId, targetId, revoked);
        } else {
            log.info("Quản trị {} MỞ KHOÁ tài khoản {}", currentAdminId, targetId);
        }
        return AdminUserResponse.from(user);
    }

    private User require(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy người dùng"));
    }
}
