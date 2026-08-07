package com.datn.quizai.ai.provider;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Ghi nhận "nhà cung cấp AI đang chặn vì hết hạn mức, tới khoảng lúc nào thì gọi lại được".
 * <p>
 * <b>Vì sao cần:</b> khi vướng 429, tác vụ nền chờ đúng thời gian provider đề nghị — có thể tới một
 * phút mỗi lần. Người học nhìn màn hình chỉ thấy một cái vòng quay "AI đang chấm" và không có cách
 * nào biết là 3 giây hay 6 phút. Hệ thống <i>biết</i> con số đó; không nói ra là để người dùng tự
 * đoán, mà đoán sai thì họ đóng trang và tưởng hỏng.
 * <p>
 * <b>Vì sao ở Redis chứ không phải cột trong CSDL:</b> đây là trạng thái tức thời, tự hết hạn, và
 * dùng chung cho mọi tác vụ AI — không gắn với riêng bài làm nào. Đặt TTL đúng bằng thời gian còn
 * phải chờ thì key tự biến mất, không cần dọn.
 * <p>
 * <b>Phạm vi:</b> hạn mức tính theo <i>khoá API</i>, mà cả cụm dùng chung một khoá, nên trạng thái
 * này đúng cho mọi instance — đó cũng là lý do để ở Redis thay vì một biến trong bộ nhớ.
 */
@Component
public class AiThrottleState {

    private static final String KEY = "ai-throttle-until";

    /** Cộng thêm chút đệm để key không hết hạn trước thời điểm nó mô tả. */
    private static final Duration TTL_SLACK = Duration.ofSeconds(5);

    private final StringRedisTemplate redis;

    public AiThrottleState(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** Provider vừa bảo chờ {@code retryAfterMillis} nữa. */
    public void markThrottled(long retryAfterMillis) {
        if (retryAfterMillis <= 0) {
            return;
        }
        long until = System.currentTimeMillis() + retryAfterMillis;
        redis.opsForValue().set(KEY, String.valueOf(until),
                Duration.ofMillis(retryAfterMillis).plus(TTL_SLACK));
    }

    /** Gọi được rồi thì xoá ngay, không đợi TTL — người dùng thôi thấy thông báo chờ sớm hơn. */
    public void clear() {
        redis.delete(KEY);
    }

    /**
     * @return số giây còn phải chờ, 0 nếu đang gọi được bình thường
     */
    public int secondsRemaining() {
        String value = redis.opsForValue().get(KEY);
        if (value == null) {
            return 0;
        }
        try {
            long remaining = Long.parseLong(value) - Instant.now().toEpochMilli();
            return remaining <= 0 ? 0 : (int) Math.ceil(remaining / 1000.0);
        } catch (NumberFormatException e) {
            // Giá trị rác thì coi như không bị chặn, còn hơn ném lỗi ra giữa màn kết quả
            return 0;
        }
    }
}
