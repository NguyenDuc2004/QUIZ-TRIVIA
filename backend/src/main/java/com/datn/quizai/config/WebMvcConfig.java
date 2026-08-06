package com.datn.quizai.config;

import com.datn.quizai.file.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

/**
 * Phục vụ ảnh đã tải lên tại {@code /uploads/**}.
 * <p>
 * Tên file do server sinh từ UUID nên nội dung một đường dẫn không bao giờ đổi → cache lâu được.
 * Đặt ngoài {@code /api} để phân biệt rõ: đây là tài nguyên tĩnh công khai, không phải endpoint API.
 * <p>
 * Đọc thẳng thuộc tính cấu hình thay vì tiêm {@code FileStorageService}: lớp cấu hình web bị
 * {@code @WebMvcTest} nạp vào, mà lát cắt đó không tạo bean {@code @Service} — tiêm vào là mọi
 * test controller đều gãy.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final Path uploadRoot;

    public WebMvcConfig(@Value("${app.storage.upload-dir:uploads}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(FileStorageService.PUBLIC_PREFIX + "**")
                .addResourceLocations(uploadRoot.toUri().toString())
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic());
    }
}
