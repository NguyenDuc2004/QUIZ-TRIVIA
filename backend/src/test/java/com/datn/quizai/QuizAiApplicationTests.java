package com.datn.quizai;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Kiểm tra Spring context khởi động được với đầy đủ PostgreSQL, Neo4j, Redis.
 * Yêu cầu: đã chạy `docker compose up -d` ở thư mục gốc dự án.
 */
@SpringBootTest
class QuizAiApplicationTests {

    @Test
    void contextLoads() {
    }
}
