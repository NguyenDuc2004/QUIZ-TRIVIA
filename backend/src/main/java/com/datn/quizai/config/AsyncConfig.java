package com.datn.quizai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Bể luồng cho tác vụ AI chạy nền (docs/conventions.md §1 — Async).
 * <p>
 * Đặt bể <b>riêng</b> thay vì dùng executor mặc định của Spring: tác vụ AI chạy hàng chục giây
 * tới vài phút, để chung với các tác vụ ngắn thì một lần nạp học liệu lớn có thể chiếm hết luồng.
 * <p>
 * Số luồng cố ý để thấp: nút cổ chai là hạn mức gọi API của nhà cung cấp, không phải CPU của mình.
 * Chạy song song nhiều hơn chỉ khiến bị trả 429 sớm hơn.
 */
@Configuration
@EnableAsync
// Bật lịch chạy định kỳ. Thiếu annotation này thì `@Scheduled` bị bỏ qua HOÀN TOÀN mà không có cảnh báo nào
// — job không chạy và không có lỗi nào để lần ra.
// Dùng bởi: job nhắc ôn tập (features/16, FR-66) và job chốt mùa (features/15, FR-63).
@EnableScheduling
public class AsyncConfig {

    /**
     * Mặc định <b>một luồng</b> — tức là các tác vụ AI xếp hàng chứ không chạy song song.
     * <p>
     * Nghe như tự bó tay, nhưng với nhà cung cấp giới hạn theo phút (Gemini bản miễn phí: 5
     * lượt/phút) thì song song chẳng được gì: hai job cùng thức dậy sau một lần chờ 429 sẽ lại
     * cùng bắn và một trong hai lại 429. Đo thật thấy đúng vậy — chấm bài và sinh đề chạy chồng
     * nhau thì cả hai cùng hỏng, chạy nối đuôi thì cả hai cùng xong.
     * <p>
     * Nâng lên khi chuyển sang gói trả phí có hạn mức cao: {@code app.ai.async.pool-size}.
     */
    @Bean("aiTaskExecutor")
    TaskExecutor aiTaskExecutor(
            @org.springframework.beans.factory.annotation.Value("${app.ai.async.pool-size:1}") int poolSize) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(poolSize);
        executor.setMaxPoolSize(poolSize);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("ai-job-");
        // Hàng đợi đầy thì chạy ngay trên luồng gọi, chậm nhưng không mất việc
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
