package com.datn.quizai.config;

import com.datn.quizai.user.domain.Role;
import com.datn.quizai.user.domain.User;
import com.datn.quizai.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tạo tài khoản quản trị <b>đầu tiên</b> khi hệ thống chưa có ai là ADMIN.
 *
 * <h3>Vấn đề: không có đường nào tạo admin đầu tiên</h3>
 * {@code AuthService} cố ý hạ mọi yêu cầu tự đăng ký ADMIN xuống LEARNER (docs/security.md §1), còn
 * đổi vai trò thì phải là admin. Trên một cơ sở dữ liệu mới tinh, hai luật đó khoá lẫn nhau: <b>không
 * có admin nào, và không có cách nào tạo admin</b>. Cách duy nhất trước đây là gõ tay
 * {@code UPDATE users SET role='ADMIN'} — một bước không ai nhớ, không nằm trong tài liệu nào, và
 * phải làm lại mỗi lần dựng máy mới hoặc mất dữ liệu.
 *
 * <h3>Vì sao KHÔNG seed bằng Flyway</h3>
 * Cách nhanh nhất là một migration {@code INSERT} sẵn admin kèm chuỗi bcrypt. Nhưng chuỗi đó nằm
 * trong repo, tức <b>mật khẩu quản trị được commit</b> — ai đọc mã nguồn cũng đăng nhập được, và đây
 * là đồ án sẽ nộp cho hội đồng. CLAUDE.md cấm commit secret, và một hash bcrypt của một mật khẩu cố
 * định thì vẫn là secret. Migration lại còn không sửa được sau khi commit, nên cũng không đổi mật
 * khẩu ấy đi được.
 *
 * Nên thông tin lấy từ biến môi trường (`.env` đã gitignore), và `.env.example` chỉ ghi tên khoá.
 *
 * <h3>Ba điều kiện để nó chạy, và vì sao đủ an toàn</h3>
 * <ol>
 *   <li><b>Chỉ khi hệ thống có ĐÚNG 0 admin.</b> Đã có một admin rồi thì bộ này im lặng bỏ qua — nên
 *       nó không dùng được để leo thang về sau. Đây là điều kiện quan trọng nhất.</li>
 *   <li><b>Chỉ khi cả email lẫn mật khẩu đều được khai.</b> Khai nửa vời thì <b>dừng hẳn ứng dụng</b>
 *       thay vì bỏ qua trong im lặng: người vận hành nghĩ mình đã cấu hình xong, mà thực tế vẫn không
 *       có admin nào — và họ chỉ phát hiện lúc cần đăng nhập.</li>
 *   <li><b>Chạy đúng một lần và lặp lại được.</b> Khởi động lại không tạo thêm tài khoản nào.</li>
 * </ol>
 *
 * <h3>Nếu email đó đã là một tài khoản thường</h3>
 * Thì <b>nâng quyền</b> nó, và ghi log mức {@code WARN} kèm email. Cân nhắc ở đây: nâng quyền một tài
 * khoản có sẵn bằng biến môi trường nghe như một đường leo thang. Nhưng điều kiện (1) đã chặn phần
 * nguy hiểm — việc này chỉ xảy ra khi hệ thống <i>chưa có admin nào</i>, tức nó đang ở trạng thái
 * chưa dựng xong chứ không phải trạng thái vận hành bình thường. Chọn từ chối cho "an toàn" thì để
 * lại đúng cái bế tắc mà lớp này sinh ra để gỡ.
 *
 * <h3>Không bao giờ ghi mật khẩu ra log</h3>
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    /** Ngắn hơn mức này thì tài khoản quyền cao nhất hệ thống lại yếu hơn tài khoản người học. */
    private static final int MAT_KHAU_TOI_THIEU = 8;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String matKhau;

    public AdminBootstrap(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          @Value("${app.admin.email:}") String email,
                          @Value("${app.admin.password:}") String matKhau) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email == null ? "" : email.trim();
        this.matKhau = matKhau == null ? "" : matKhau;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.countByRole(Role.ADMIN) > 0) {
            return;
        }

        if (email.isBlank() && matKhau.isBlank()) {
            // Chưa khai gì: không phải lỗi, chỉ là chưa dùng tính năng này. Nhưng vẫn phải nói ra —
            // hệ thống KHÔNG có admin nào, và người vận hành cần biết điều đó ngay lúc khởi động chứ
            // không phải lúc họ cần vào khu quản trị.
            log.warn("Hệ thống chưa có tài khoản ADMIN nào. Khai APP_ADMIN_EMAIL và "
                    + "APP_ADMIN_PASSWORD trong .env rồi khởi động lại để tạo admin đầu tiên.");
            return;
        }

        if (email.isBlank() || matKhau.isBlank()) {
            throw new IllegalStateException(
                    "APP_ADMIN_EMAIL và APP_ADMIN_PASSWORD phải khai CẢ HAI hoặc bỏ trống cả hai. "
                            + "Khai một nửa thì không tạo được admin, mà người vận hành lại tưởng đã xong.");
        }

        if (matKhau.length() < MAT_KHAU_TOI_THIEU) {
            throw new IllegalStateException(
                    "APP_ADMIN_PASSWORD phải dài ít nhất " + MAT_KHAU_TOI_THIEU + " ký tự.");
        }

        userRepository.findByEmail(email).ifPresentOrElse(
                daCo -> {
                    daCo.setRole(Role.ADMIN);
                    log.warn("Nâng tài khoản sẵn có {} lên ADMIN vì hệ thống chưa có admin nào. "
                            + "Nếu đây không phải ý bạn, hãy đổi APP_ADMIN_EMAIL.", email);
                },
                () -> {
                    userRepository.save(new User(
                            email, passwordEncoder.encode(matKhau), "Quản trị hệ thống", Role.ADMIN));
                    log.info("Đã tạo tài khoản ADMIN đầu tiên: {}", email);
                });
    }
}
