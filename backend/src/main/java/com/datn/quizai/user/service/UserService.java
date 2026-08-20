package com.datn.quizai.user.service;

import com.datn.quizai.user.domain.User;
import com.datn.quizai.user.repository.UserRepository;
import com.datn.quizai.common.exception.BusinessException;
import com.datn.quizai.file.service.UploadedImagePath;
import com.datn.quizai.user.dto.UpdateProfileRequest;
import com.datn.quizai.user.dto.UserResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(UUID userId) {
        return UserResponse.from(findOrThrow(userId));
    }

    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = findOrThrow(userId);
        user.setDisplayName(request.displayName().trim());
        user.setAvatarUrl(anhDaiDienHopLe(request.avatarUrl(), user.getAvatarUrl()));
        return UserResponse.from(user);
    }

    /**
     * Kiểm ảnh đại diện người dùng gửi lên — cùng luật an toàn với ảnh bìa quiz và ảnh câu hỏi.
     *
     * <h4>Vì sao trước đây nhận URL bất kỳ là một lỗ hổng</h4>
     * Ảnh đại diện hiện trên <b>thanh điều hướng, bảng xếp hạng, danh sách thành viên lớp và thẻ người chơi
     * trong phòng đấu</b> — tức nó được tải trên màn hình của <i>người khác</i>. Đặt một URL ngoài nghĩa là
     * mỗi người nhìn thấy tên bạn sẽ gửi một request kèm IP tới máy chủ do bạn chọn. Đó là theo dõi người
     * dùng khác qua một ô nhập tưởng như vô hại.
     *
     * <h4>Nhưng KHÔNG được chặn ảnh Google</h4>
     * Đăng nhập Google lưu sẵn ảnh từ CDN của Google ({@code lh3.googleusercontent.com}) — một URL ngoài
     * hoàn toàn hợp lệ, do <b>máy chủ</b> ghi lúc đăng nhập chứ không phải người dùng gửi lên. Chặn cứng thì
     * người đăng nhập bằng Google chỉ đổi tên hiển thị thôi cũng bị từ chối, vì form gửi kèm ảnh hiện có.
     * <p>
     * Nên luật là: <b>giữ nguyên thì luôn được, đổi thì phải là ảnh đã tải lên hệ thống này</b>.
     */
    private String anhDaiDienHopLe(String moi, String hienTai) {
        if (java.util.Objects.equals(moi, hienTai)) {
            return hienTai;
        }
        return UploadedImagePath.hopLeHoacNull(moi, "Ảnh đại diện");
    }

    private User findOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy người dùng"));
    }
}
