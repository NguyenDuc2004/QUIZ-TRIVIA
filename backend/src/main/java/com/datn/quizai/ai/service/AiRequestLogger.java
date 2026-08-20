package com.datn.quizai.ai.service;

import com.datn.quizai.ai.provider.AiCompletion;
import com.datn.quizai.ai.provider.AiProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
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

    /**
     * Đếm số lời gọi của một người từ mốc thời gian trở đi — nguồn sự thật để dựng lại bộ đếm hạn mức
     * (FR-84) khi Redis rỗng.
     * <p>
     * Đặt ở đây chứ không ở một repository mới: lớp này đã sở hữu bảng `ai_request_logs`, và tách câu SQL
     * của cùng một bảng ra hai chỗ là mở đường cho hai chỗ hiểu khác nhau về nó.
     * <p>
     * Đếm <b>cả bản ghi FAILED</b>: một lời gọi hỏng vẫn tốn hạn mức của nhà cung cấp, và nếu không tính
     * thì một người có thể gửi prompt sai định dạng vô hạn lần mà không bao giờ chạm hạn mức.
     */
    public long demTuThoiDiem(UUID userId, OffsetDateTime tu) {
        Long n = jdbc.queryForObject(
                "select count(*) from ai_request_logs where user_id = ? and created_at >= ?",
                Long.class, userId, tu);
        return n == null ? 0 : n;
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
