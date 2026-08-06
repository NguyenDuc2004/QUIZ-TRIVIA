package com.datn.quizai.auth.service;

import com.datn.quizai.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;

/**
 * Refresh token đối xứng lưu ở Redis theo key {@code session:{token}} (docs/database.md §3).
 * <p>
 * Áp dụng <b>rotation</b>: mỗi lần làm mới sẽ thu hồi token cũ và phát token mới,
 * nên một refresh token chỉ dùng được một lần (docs/security.md §1).
 * <p>
 * Ngoài ra giữ một tập <b>chỉ mục ngược</b> {@code user-sessions:{userId}} liệt kê mọi token đang
 * hoạt động của một người. Không có tập này thì muốn thu hồi hết phiên của một người phải
 * {@code SCAN} toàn bộ key {@code session:*} — chậm và không đáng, trong khi việc đó cần thiết cho
 * hai tình huống thật: đổi mật khẩu, và "đăng xuất mọi thiết bị" khi mất máy.
 */
@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    private static final String KEY_PREFIX = "session:";
    private static final String USER_SESSIONS_PREFIX = "user-sessions:";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redis;
    private final Duration refreshTtl;

    public RefreshTokenService(StringRedisTemplate redis,
                               @Value("${app.jwt.refresh-ttl-seconds}") long refreshTtlSeconds) {
        this.redis = redis;
        this.refreshTtl = Duration.ofSeconds(refreshTtlSeconds);
    }

    public String issue(UUID userId) {
        byte[] bytes = new byte[48];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        redis.opsForValue().set(KEY_PREFIX + token, userId.toString(), refreshTtl);

        // Chỉ mục ngược để thu hồi được cả loạt. Gia hạn TTL mỗi lần phát token mới nên tập này
        // không bao giờ sống lâu hơn token cuối cùng của người đó.
        String indexKey = USER_SESSIONS_PREFIX + userId;
        redis.opsForSet().add(indexKey, token);
        redis.expire(indexKey, refreshTtl);

        return token;
    }

    /** @return id người dùng gắn với token, hoặc lỗi 401 nếu token không tồn tại/đã hết hạn */
    public UUID resolve(String token) {
        String userId = redis.opsForValue().get(KEY_PREFIX + token);
        if (userId == null) {
            throw BusinessException.unauthorized("Refresh token không hợp lệ hoặc đã hết hạn");
        }
        return UUID.fromString(userId);
    }

    /** Thu hồi token cũ rồi phát token mới cho cùng người dùng. */
    public String rotate(String oldToken) {
        UUID userId = resolve(oldToken);
        revoke(oldToken);
        return issue(userId);
    }

    /** Thu hồi <b>một</b> phiên — dùng khi đăng xuất trên đúng thiết bị đang gọi. */
    public void revoke(String token) {
        // Đọc userId trước khi xoá để còn dọn được chỉ mục ngược
        String userId = redis.opsForValue().get(KEY_PREFIX + token);
        redis.delete(KEY_PREFIX + token);

        if (userId != null) {
            redis.opsForSet().remove(USER_SESSIONS_PREFIX + userId, token);
        }
    }

    /**
     * Thu hồi <b>mọi</b> phiên của một người, trên tất cả thiết bị.
     * <p>
     * Gọi khi đổi mật khẩu và khi người dùng chủ động "đăng xuất mọi thiết bị". Không có bước này,
     * người bị mất điện thoại đổi mật khẩu xong vẫn tưởng đã cắt truy cập, trong khi chiếc điện
     * thoại đó còn vào được tới hết hạn refresh token.
     *
     * @return số phiên đã thu hồi
     */
    public int revokeAll(UUID userId) {
        String indexKey = USER_SESSIONS_PREFIX + userId;
        Set<String> tokens = redis.opsForSet().members(indexKey);

        if (tokens == null || tokens.isEmpty()) {
            redis.delete(indexKey);
            return 0;
        }

        redis.delete(tokens.stream().map(token -> KEY_PREFIX + token).toList());
        redis.delete(indexKey);

        log.info("Đã thu hồi {} phiên của người dùng {}", tokens.size(), userId);
        return tokens.size();
    }

    public long refreshTtlSeconds() {
        return refreshTtl.toSeconds();
    }
}
