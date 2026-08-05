package com.datn.quizai.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Tài liệu API (Swagger UI tại /swagger-ui.html) + nút Authorize để dán Bearer token. */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI quizAiOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Quiz/Trivia AI API")
                        .version("v1")
                        .description("API của ứng dụng Quiz/Trivia tích hợp trí tuệ nhân tạo"))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Dán access token lấy từ POST /api/v1/auth/login")));
    }
}
