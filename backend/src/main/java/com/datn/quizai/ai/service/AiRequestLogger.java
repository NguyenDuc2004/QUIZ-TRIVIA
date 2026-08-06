package com.datn.quizai.ai.service;

import com.datn.quizai.ai.provider.AiCompletion;
import com.datn.quizai.ai.provider.AiProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Ghi audit mọi lời gọi LLM vào bảng `ai_request_logs`.
 * <p>
 * Đây là nguồn số liệu cho <b>mục 3.6 báo cáo</b> (chi phí, độ trễ, tỉ lệ fallback) và cũng là
 * cách duy nhất biết provider nào thực sự phục vụ một kết quả.
 * <p>
 * Chạy trong transaction <b>riêng</b> ({@code REQUIRES_NEW}): job sinh đề hỏng và rollback thì
 * bản ghi audit vẫn phải còn — mất đúng cái log của lần lỗi là mất thứ cần nhất khi đi tìm nguyên nhân.
 */
@Service
public class AiRequestLogger {

    private static final Logger log = LoggerFactory.getLogger(AiRequestLogger.class);

    private final JdbcTemplate jdbc;

    public AiRequestLogger(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSuccess(UUID userId, String feature, AiCompletion completion) {
        insert(userId, feature, completion.provider(), completion.model(),
                completion.tokensIn(), completion.tokensOut(),
                (int) completion.latencyMs(), "SUCCESS", null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logFailure(UUID userId, String feature, AiProvider provider, String error) {
        insert(userId, feature, provider.name(), provider.model(), null, null, null, "FAILED", error);
    }

    private void insert(UUID userId, String feature, String provider, String model,
                        Integer tokensIn, Integer tokensOut, Integer latencyMs,
                        String status, String error) {
        try {
            jdbc.update("""
                            insert into ai_request_logs
                                (id, user_id, feature, provider, model, tokens_in, tokens_out,
                                 latency_ms, status, error_message)
                            values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    UUID.randomUUID(), userId, feature, provider, model,
                    tokensIn, tokensOut, latencyMs, status, error);

        } catch (RuntimeException e) {
            // Ghi audit hỏng thì chỉ log lại, không được làm hỏng tính năng chính
            log.error("Không ghi được audit lời gọi AI ({} / {})", feature, provider, e);
        }
    }
}
