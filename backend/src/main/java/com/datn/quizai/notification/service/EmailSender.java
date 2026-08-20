package com.datn.quizai.notification.service;

import com.datn.quizai.notification.domain.NotificationType;
import com.datn.quizai.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Gửi thông báo qua email (features/16, FR-69).
 *
 * <h3>Mặc định TẮT, và đó là hành vi đúng chứ không phải cấu hình thiếu</h3>
 * Không đặt {@code spring.mail.host} thì lớp này không gửi gì cả, và hệ thống chạy y như trước khi có nó:
 * thông báo nằm trong ứng dụng. Thêm một phụ thuộc rồi bật sẵn là đổi hành vi của hệ thống đang chạy mà
 * không ai chọn — và trong trường hợp này là bắt đầu gửi thư ra ngoài nhân danh người dùng.
 *
 * <h3>Email là BẢN SAO, không phải kênh thay thế</h3>
 * Thông báo luôn được ghi vào cơ sở dữ liệu trước và luôn hiện trong ứng dụng. Email chỉ là một bản gửi
 * thêm cho người không mở ứng dụng thường xuyên. Nhờ vậy email hỏng — SMTP sập, hộp thư đầy, thư vào spam —
 * <b>không làm mất thông báo</b>: nó vẫn nằm nguyên trong ứng dụng.
 * <p>
 * Đây là lý do lớp này {@code @Async} và <b>nuốt mọi lỗi</b>. Một máy chủ SMTP chậm không được phép làm
 * chậm việc nộp bài hay việc chạy job nhắc ôn, và một lần gửi hỏng không được phép làm rollback thông báo
 * đã ghi.
 *
 * <h3>Điều KHÔNG kiểm chứng được, và đặc tả đã nói trước</h3>
 * Lý do hoãn FR-69 ban đầu: *"gửi thành công ở phía mình không nói được gì về việc thư có tới"*. Điều đó
 * vẫn đúng. Test dùng một máy chủ SMTP trong bộ nhớ nên chứng minh được **thư soạn đúng và gửi đúng giao
 * thức**; nó <b>không</b> chứng minh thư vào hộp thư đến thay vì thư rác — chuyện đó phụ thuộc danh tiếng
 * tên miền người gửi, nằm ngoài phạm vi đồ án.
 */
@Service
public class EmailSender {

    private static final Logger log = LoggerFactory.getLogger(EmailSender.class);

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;
    private final String taiKhoanGui;
    private final String nguoiGui;

    public EmailSender(JavaMailSender mailSender,
                       UserRepository userRepository,
                       @Value("${spring.mail.username:}") String taiKhoanGui,
                       @Value("${app.mail.from:}") String nguoiGui) {
        this.mailSender = mailSender;
        this.userRepository = userRepository;
        this.taiKhoanGui = taiKhoanGui == null ? "" : taiKhoanGui.trim();
        // Bỏ trống `app.mail.from` thì dùng chính tài khoản gửi — cấu hình sẵn có của dự án đã nói vậy
        this.nguoiGui = (nguoiGui == null || nguoiGui.isBlank()) ? this.taiKhoanGui : nguoiGui.trim();
    }

    /**
     * Đã cấu hình tài khoản gửi hay chưa.
     *
     * <h4>Dựa vào `username`, KHÔNG dựa vào `host`</h4>
     * `spring.mail.host` trong dự án này có <b>giá trị mặc định</b> ({@code smtp.gmail.com}) vì nó vốn được
     * cấu hình sẵn cho OTP đặt lại mật khẩu. Lấy host làm dấu hiệu bật/tắt thì tính năng luôn "đang bật" và
     * hệ thống sẽ cố gửi thư ngay lần chạy đầu tiên — đúng thứ mà "mặc định tắt" muốn tránh.
     * <p>
     * Không có tài khoản gửi thì không gửi được gì, dù host trỏ đúng máy chủ Gmail.
     */
    public boolean daBat() {
        return !taiKhoanGui.isBlank();
    }

    /**
     * Gửi một thông báo qua email, nếu tính năng đang bật.
     * <p>
     * Chạy nền: người dùng không phải chờ SMTP trả lời mới thấy màn hình phản hồi.
     */
    @Async
    public void gui(UUID userId, NotificationType type, String tieuDe, String noiDung) {
        if (!daBat()) {
            return;
        }
        try {
            String email = userRepository.findById(userId).map(u -> u.getEmail()).orElse(null);
            if (email == null || email.isBlank()) {
                return;
            }

            SimpleMailMessage thu = new SimpleMailMessage();
            thu.setFrom(nguoiGui);
            thu.setTo(email);
            // Tiền tố loại thông báo ngay ở tiêu đề: hộp thư của người học có hàng chục thư mỗi ngày, và
            // "Nhắc ôn tập — ..." lọc được bằng mắt, còn "Thông báo mới" thì không.
            thu.setSubject("[" + type.nhan() + "] " + tieuDe);
            thu.setText(noiDung == null ? tieuDe : noiDung);

            mailSender.send(thu);
            log.debug("Đã gửi email {} tới {}", type, userId);

        } catch (Exception e) {
            // Nuốt có chủ đích: thông báo đã nằm trong cơ sở dữ liệu và đã hiện trong ứng dụng. Ném lỗi ra
            // ngoài chỉ làm hỏng thứ đang chạy đúng, để đổi lấy một bản sao không gửi được.
            log.warn("Không gửi được email tới {}: {}", userId, e.getMessage());
        }
    }
}
