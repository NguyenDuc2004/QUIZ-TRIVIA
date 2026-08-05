package com.datn.quizai.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Nạp file {@code .env} ở gốc dự án vào Environment khi chạy local, để không phải
 * khai báo tay biến môi trường mỗi lần chạy `./mvnw spring-boot:run` hay chạy test.
 * <p>
 * Thứ tự ưu tiên: biến môi trường thật của hệ điều hành / tham số dòng lệnh
 * <b>luôn thắng</b> giá trị trong {@code .env} (property source này được thêm ở mức thấp nhất),
 * nên khi triển khai thật vẫn dùng biến môi trường / secret manager như docs/security.md §3.
 * <p>
 * Tìm {@code .env} tại thư mục làm việc rồi tới thư mục cha (vì backend chạy trong
 * {@code backend/} còn {@code .env} nằm ở gốc repo).
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String PROPERTY_SOURCE_NAME = "dotenv";
    private static final List<Path> CANDIDATES = List.of(Path.of(".env"), Path.of("..", ".env"));

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        for (Path candidate : CANDIDATES) {
            if (Files.isRegularFile(candidate)) {
                Map<String, Object> values = parse(candidate);
                if (!values.isEmpty()) {
                    // addLast: mọi nguồn khác (env thật, --args, application.yml) đều ưu tiên hơn
                    environment.getPropertySources()
                            .addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, values));
                }
                return;
            }
        }
    }

    private Map<String, Object> parse(Path file) {
        Map<String, Object> values = new HashMap<>();
        try {
            for (String rawLine : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String line = rawLine.strip();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int separator = line.indexOf('=');
                if (separator <= 0) {
                    continue;
                }
                String key = line.substring(0, separator).strip();
                String value = stripQuotes(line.substring(separator + 1).strip());
                values.put(key, value);
            }
        } catch (IOException ex) {
            // Không chặn khởi động chỉ vì đọc .env lỗi — cấu hình vẫn có thể đến từ env thật
            System.err.println("Không đọc được " + file.toAbsolutePath() + ": " + ex.getMessage());
        }
        return values;
    }

    private String stripQuotes(String value) {
        if (value.length() >= 2
                && ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'")))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
