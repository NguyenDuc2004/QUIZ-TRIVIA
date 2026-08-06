package com.datn.quizai.auth.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

/**
 * Gửi email giao dịch (hiện chỉ có mã OTP đặt lại mật khẩu — FR-4).
 * <p>
 * Dùng SMTP của Gmail với <b>App Password</b>: Google đã chặn đăng nhập SMTP bằng mật khẩu tài
 * khoản từ 2022, nên phải bật xác minh 2 bước rồi tạo mã ứng dụng 16 ký tự riêng.
 * <p>
 * <b>Chưa cấu hình thì không ném lỗi ra ngoài</b> mà chỉ ghi log: người dùng bấm "Quên mật khẩu"
 * sẽ nhận cùng một phản hồi dù email có tồn tại hay không (xem {@code AuthService}), nên để lộ
 * lỗi gửi thư ở đây là gián tiếp tiết lộ email nào có trong hệ thống.
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;
    private final String username;
    private final String from;
    private final String fromName;

    public MailService(JavaMailSender mailSender,
                       @Value("${spring.mail.username:}") String username,
                       @Value("${app.mail.from:}") String from,
                       @Value("${app.mail.from-name}") String fromName) {
        this.mailSender = mailSender;
        this.username = username == null ? "" : username.trim();
        this.from = from == null || from.isBlank() ? this.username : from.trim();
        this.fromName = fromName;
    }

    public boolean isConfigured() {
        return !username.isBlank();
    }

    /** @return true nếu thư đã được giao cho máy chủ SMTP */
    public boolean sendPasswordResetOtp(String toEmail, String displayName, String code, long ttlMinutes) {
        if (!isConfigured()) {
            log.warn("Chưa cấu hình MAIL_USERNAME/MAIL_PASSWORD — không gửi được mã OTP. "
                    + "Mã dành cho {} là: {} (chỉ hiện trong log khi chưa cấu hình mail)", toEmail, code);
            return false;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

            helper.setFrom(from, fromName);
            helper.setTo(toEmail);
            helper.setSubject("Mã đặt lại mật khẩu Quiz AI: " + code);
            helper.setText(buildHtml(displayName, code, ttlMinutes), true);

            mailSender.send(message);
            log.info("Đã gửi mã OTP tới {}", toEmail);
            return true;

        } catch (UnsupportedEncodingException | jakarta.mail.MessagingException e) {
            log.error("Không dựng được email gửi tới {}", toEmail, e);
            return false;
        } catch (RuntimeException e) {
            // Sai App Password, mạng chặn cổng 587, Gmail từ chối… — không để lộ ra client
            log.error("Gửi email tới {} thất bại: {}", toEmail, e.getMessage());
            return false;
        }
    }

    /**
     * Email HTML viết bằng inline style: phần lớn ứng dụng mail (nhất là Gmail) cắt bỏ thẻ
     * {@code <style>}, nên CSS phải nằm ngay trên từng thẻ.
     */
    private String buildHtml(String displayName, String code, long ttlMinutes) {
        return """
                <div style="font-family:Arial,Helvetica,sans-serif;max-width:520px;margin:0 auto;
                            color:#1c1d1f;line-height:1.6">
                  <p style="font-size:22px;font-weight:800;margin:0 0 24px">
                    Quiz<span style="color:#a435f0">AI</span>
                  </p>

                  <p style="margin:0 0 8px">Chào %s,</p>
                  <p style="margin:0 0 20px">
                    Bạn vừa yêu cầu đặt lại mật khẩu. Nhập mã dưới đây để tiếp tục:
                  </p>

                  <p style="font-size:34px;font-weight:800;letter-spacing:10px;text-align:center;
                            background:#f7f9fa;border:1px solid #d1d7dc;padding:18px;margin:0 0 20px">
                    %s
                  </p>

                  <p style="margin:0 0 20px;color:#6a6f73;font-size:14px">
                    Mã có hiệu lực trong <b>%d phút</b> và chỉ dùng được một lần.
                  </p>

                  <p style="margin:0 0 4px;color:#6a6f73;font-size:14px">
                    Nếu bạn <b>không</b> yêu cầu đặt lại mật khẩu, hãy bỏ qua email này —
                    mật khẩu của bạn không thay đổi.
                  </p>
                  <p style="margin:24px 0 0;color:#6a6f73;font-size:12px;border-top:1px solid #d1d7dc;
                            padding-top:12px">
                    Đây là email tự động, vui lòng không trả lời.
                  </p>
                </div>
                """.formatted(escape(displayName), code, ttlMinutes);
    }

    /** Tên hiển thị do người dùng tự đặt — phải escape, nếu không là lỗ hổng chèn HTML vào email. */
    private String escape(String value) {
        if (value == null) {
            return "bạn";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
