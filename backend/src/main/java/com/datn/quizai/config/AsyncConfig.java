package com.datn.quizai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
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
public class AsyncConfig {

    @Bean("aiTaskExecutor")
    TaskExecutor aiTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("ai-job-");
        // Hàng đợi đầy thì chạy ngay trên luồng gọi, chậm nhưng không mất việc
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
