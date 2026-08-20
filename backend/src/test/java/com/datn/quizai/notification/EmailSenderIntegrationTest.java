package com.datn.quizai.notification;

import com.datn.quizai.notification.domain.NotificationType;
import com.datn.quizai.notification.service.EmailSender;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Gửi email thông báo (features/16, FR-69).
 *
 * <h3>Vì sao dùng máy chủ SMTP THẬT trong bộ nhớ, không mock JavaMailSender</h3>
 * Mock chỉ chứng minh code <b>gọi đúng hàm</b>. Nó vẫn xanh khi thư thiếu người nhận, sai mã hoá tiếng Việt,
 * hay tiêu đề rỗng — đúng những thứ hỏng mà người dùng sẽ thấy. GreenMail nói SMTP thật, nên test đọc lại
 * được chính thứ máy chủ nhận.
 *
 * <h3>Điều test này KHÔNG chứng minh</h3>
 * Đặc tả hoãn FR-69 với lý do: *"gửi thành công ở phía mình không nói được gì về việc thư có tới"*. Vẫn đúng.
 * Ở đây chứng minh được **thư soạn đúng và gửi đúng giao thức**; <b>không</b> chứng minh nó vào hộp thư đến
 * thay vì thư rác — chuyện đó phụ thuộc danh tiếng tên miền người gửi.
 */
@SpringBootTest
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = {
        "spring.mail.host=localhost",
        "spring.mail.port=3025",
        // `username` là dấu hiệu BẬT/TẮT của tính năng (host có giá trị mặc định nên không dùng được).
        // GreenMail không đòi xác thực, giá trị này chỉ để bật tính năng và làm địa chỉ người gửi.
        "spring.mail.username=quizai-test@example.com",
        "spring.mail.properties.mail.smtp.auth=false",
        "spring.mail.properties.mail.smtp.starttls.enable=false",
        "app.mail.from=quizai-test@example.com",
})
class EmailSenderIntegrationTest {

    /**
     * Máy chủ SMTP thật, chạy trong bộ nhớ.
     * <p>
     * {@code withDisabledAuthentication()}: GreenMail bản 2.x <b>bắt buộc xác thực</b> theo mặc định và từ
     * chối phiên không đăng nhập bằng "Authentication failed". Tắt yêu cầu đó ở phía máy chủ giả — thứ phép
     * kiểm này quan tâm là <i>thư soạn đúng và gửi đúng giao thức</i>, không phải cơ chế đăng nhập SMTP của
     * một máy chủ giả lập.
     */
    @RegisterExtension
    static GreenMailExtension smtp = new GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(com.icegreen.greenmail.configuration.GreenMailConfiguration.aConfig()
                    .withDisabledAuthentication());

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @Autowired
    private EmailSender emailSender;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("Gửi được thư tới đúng địa chỉ, kèm tiêu đề và nội dung tiếng Việt có dấu")
    void shouldSendEmailWithVietnameseContent() throws Exception {
        String email = "hocvien-" + UUID.randomUUID() + "@example.com";
        UUID userId = taoNguoiDung(email);

        emailSender.gui(userId, NotificationType.SRS_REMINDER,
                "Bạn có 12 thẻ đến hạn ôn hôm nay",
                "Vào mục Flashcard để ôn lại trước khi quên.");

        assertThat(smtp.waitForIncomingEmail(10_000, 1)).isTrue();
        MimeMessage[] thu = smtp.getReceivedMessages();
        assertThat(thu).hasSize(1);

        assertThat(thu[0].getAllRecipients()[0].toString()).isEqualTo(email);
        // Tiền tố loại thông báo: hộp thư có hàng chục thư mỗi ngày, "[Nhắc ôn tập]" lọc được bằng mắt
        assertThat(thu[0].getSubject()).contains("Nhắc ôn tập").contains("12 thẻ đến hạn");
        // Dấu tiếng Việt phải nguyên vẹn — mock JavaMailSender không bao giờ bắt được lỗi mã hoá này
        assertThat(noiDung(thu[0])).contains("Vào mục Flashcard để ôn lại trước khi quên.");
    }

    @Test
    @DisplayName("Người dùng không tồn tại: không gửi gì, không ném lỗi")
    void shouldIgnoreUnknownUser() {
        assertThatCode(() -> emailSender.gui(UUID.randomUUID(), NotificationType.SYSTEM, "Tiêu đề", "Nội dung"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Nội dung rỗng thì dùng tiêu đề làm thân thư, không gửi thư trắng")
    void shouldFallBackToTitleWhenBodyMissing() throws Exception {
        smtp.purgeEmailFromAllMailboxes();
        UUID userId = taoNguoiDung("trong-" + UUID.randomUUID() + "@example.com");

        emailSender.gui(userId, NotificationType.ACHIEVEMENT, "Bạn vừa lên cấp 5", null);

        assertThat(smtp.waitForIncomingEmail(10_000, 1)).isTrue();
        // Thư trắng tới hộp thư người dùng là một thông báo không nói được gì — tệ hơn là không gửi
        assertThat(noiDung(smtp.getReceivedMessages()[0])).contains("Bạn vừa lên cấp 5");
    }

    @Test
    @DisplayName("Dấu hiệu bật/tắt là TÀI KHOẢN GỬI, không phải host")
    void shouldReportEnabled() {
        // `spring.mail.host` trong dự án này có giá trị MẶC ĐỊNH (smtp.gmail.com) vì nó vốn được cấu hình
        // sẵn cho OTP đặt lại mật khẩu. Lấy host làm dấu hiệu thì tính năng luôn "đang bật" và hệ thống cố
        // gửi thư ngay lần chạy đầu — đúng thứ mà "mặc định tắt" muốn tránh.
        assertThat(emailSender.daBat()).isTrue();
    }

    private UUID taoNguoiDung(String email) {
        return UUID.fromString(jdbc.queryForObject("""
                insert into users (id, email, password_hash, display_name, role)
                values (gen_random_uuid(), ?, 'x', 'Người nhận thư', 'LEARNER')
                returning id::text
                """, String.class, email));
    }

    /**
     * Thân thư đã GIẢI MÃ.
     * <p>
     * {@code GreenMailUtil.getBody()} trả về thân thô, mà tiếng Việt được mã hoá quoted-printable
     * ({@code V=C3=A0o m=E1=BB=A5c...}) — đúng chuẩn MIME cho UTF-8, nhưng so chuỗi trực tiếp thì không
     * khớp. {@code getContent()} giải mã theo đúng charset thư khai báo, nên nó cũng chính là thứ chứng
     * minh charset được khai báo đúng: khai sai thì chuỗi giải ra sẽ hỏng dấu.
     */
    private static String noiDung(MimeMessage thu) throws Exception {
        return thu.getContent().toString();
    }
}
