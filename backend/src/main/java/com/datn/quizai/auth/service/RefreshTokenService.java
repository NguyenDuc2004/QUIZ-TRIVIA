package com.datn.quizai.auth.service;

import com.datn.quizai.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

/**
 * Refresh token đối xứng lưu ở Redis theo key {@code session:{token}} (docs/database.md §3).
 * <p>
 * Áp dụng <b>rotation</b>: mỗi lần làm mới sẽ thu hồi token cũ và phát token mới,
 * nên một refresh token chỉ dùng được một lần (docs/security.md §1).
 */
@Service
public class RefreshTokenService {

    private static final String KEY_PREFIX = "session:";
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

    public void revoke(String token) {
        redis.delete(KEY_PREFIX + token);
    }

    public long refreshTtlSeconds() {
        return refreshTtl.toSeconds();
    }
}
