package com.datn.quizai.auth.service;

import com.datn.quizai.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Locale;

/**
 * Mã OTP đặt lại mật khẩu, lưu ở Redis {@code pwd-otp:{email}} (FR-4).
 * <p>
 * Bốn biện pháp bảo vệ, mỗi cái chặn một kiểu tấn công khác nhau:
 * <ol>
 *   <li><b>OTP lưu dạng băm</b>, không lưu thô. Ai đọc được Redis (log, dump, backup) cũng không
 *       dùng lại được mã của người khác.</li>
 *   <li><b>Giới hạn số lần nhập sai</b>. Mã 6 chữ số chỉ có một triệu khả năng — không giới hạn thì
 *       dò hết trong vài phút. Sai quá số lần thì huỷ mã, bắt xin lại.</li>
 *   <li><b>Thời hạn ngắn</b>. Mã chết sau ít phút, thu hẹp cửa sổ tấn công.</li>
 *   <li><b>Giãn cách giữa hai lần xin mã</b>. Không có nó thì bất kỳ ai cũng bơm được hàng nghìn
 *       email vào hòm thư của người khác chỉ bằng cách gọi API liên tục.</li>
 * </ol>
 * <p>
 * Mỗi lần xin mã mới sẽ <b>ghi đè</b> mã cũ: người dùng bấm "Gửi lại" rồi nhập mã trong thư đầu
 * tiên là sai — đúng như mong đợi, và tránh việc nhiều mã cùng sống một lúc.
 */
@Service
public class PasswordResetOtpService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetOtpService.class);

    private static final String OTP_PREFIX = "pwd-otp:";
    private static final String ATTEMPTS_PREFIX = "pwd-otp-attempts:";
    private static final String COOLDOWN_PREFIX = "pwd-otp-cooldown:";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redis;
    private final PasswordEncoder passwordEncoder;
    private final int length;
    private final Duration ttl;
    private final int maxAttempts;
    private final Duration resendCooldown;

    public PasswordResetOtpService(StringRedisTemplate redis,
                                   PasswordEncoder passwordEncoder,
                                   @Value("${app.mail.otp.length}") int length,
                                   @Value("${app.mail.otp.ttl-minutes}") long ttlMinutes,
                                   @Value("${app.mail.otp.max-attempts}") int maxAttempts,
                                   @Value("${app.mail.otp.resend-cooldown-seconds}") long cooldownSeconds) {
        this.redis = redis;
        this.passwordEncoder = passwordEncoder;
        this.length = length;
        this.ttl = Duration.ofMinutes(ttlMinutes);
        this.maxAttempts = maxAttempts;
        this.resendCooldown = Duration.ofSeconds(cooldownSeconds);
    }

    public long ttlMinutes() {
        return ttl.toMinutes();
    }

    /**
     * Kiểm tra người này có được xin mã mới chưa.
     *
     * @throws BusinessException 429 nếu vừa xin cách đây chưa lâu
     */
    public void assertCanSend(String email) {
        String key = COOLDOWN_PREFIX + normalize(email);
        Long remaining = redis.getExpire(key);

        if (Boolean.TRUE.equals(redis.hasKey(key))) {
            throw new BusinessException(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS,
                    "Vui lòng đợi " + Math.max(1, remaining == null ? 0 : remaining)
                            + " giây nữa rồi xin mã mới");
        }
    }

    /** Sinh mã mới, lưu bản băm, đặt lại bộ đếm và mốc giãn cách. @return mã thô để gửi qua email */
    public String issue(String email) {
        String normalized = normalize(email);
        String code = randomCode();

        redis.opsForValue().set(OTP_PREFIX + normalized, passwordEncoder.encode(code), ttl);
        redis.delete(ATTEMPTS_PREFIX + normalized);
        redis.opsForValue().set(COOLDOWN_PREFIX + normalized, "1", resendCooldown);

        log.info("Đã phát mã OTP đặt lại mật khẩu cho {}", maskEmail(normalized));
        return code;
    }

    /**
     * Xác minh mã. Đúng thì <b>xoá luôn</b> mã để không dùng lại được lần hai.
     *
     * @throws BusinessException 400 nếu mã sai, hết hạn, hoặc đã sai quá số lần cho phép
     */
    public void verifyAndConsume(String email, String code) {
        String normalized = normalize(email);
        String hashed = redis.opsForValue().get(OTP_PREFIX + normalized);

        if (hashed == null) {
            throw BusinessException.badRequest("Mã xác thực không đúng hoặc đã hết hạn");
        }

        if (!passwordEncoder.matches(code == null ? "" : code.trim(), hashed)) {
            long attempts = countFailure(normalized);
            if (attempts >= maxAttempts) {
                clear(normalized);
                throw BusinessException.badRequest(
                        "Nhập sai quá " + maxAttempts + " lần, mã đã bị huỷ. Hãy xin mã mới.");
            }
            throw BusinessException.badRequest("Mã xác thực không đúng hoặc đã hết hạn"
                    + " (còn " + (maxAttempts - attempts) + " lần thử)");
        }

        clear(normalized);
    }

    private long countFailure(String normalized) {
        String key = ATTEMPTS_PREFIX + normalized;
        Long attempts = redis.opsForValue().increment(key);
        // Bộ đếm phải chết cùng lúc với mã, nếu không nó sống mãi và chặn oan lần xin mã sau
        redis.expire(key, ttl);
        return attempts == null ? 1 : attempts;
    }

    private void clear(String normalized) {
        redis.delete(OTP_PREFIX + normalized);
        redis.delete(ATTEMPTS_PREFIX + normalized);
    }

    /** Dùng {@link SecureRandom} chứ không phải {@code Math.random()} — mã đoán được là vô dụng. */
    private String randomCode() {
        StringBuilder code = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            code.append(RANDOM.nextInt(10));
        }
        return code.toString();
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    /** Che email trong log: log không phải chỗ để lộ danh sách người dùng. */
    private String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 1) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(at);
    }
}
