package com.datn.quizai.ai.provider;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test đọc gợi ý "chờ bao lâu rồi hãy gọi lại" từ thông báo lỗi 429 của Gemini.
 * <p>
 * Nhìn thì vặt, nhưng đây là thứ quyết định tính năng chấm tự luận có dùng được trên gói miễn phí
 * hay không: hạn mức là <b>5 lượt mỗi phút</b>, nên bài từ câu tự luận thứ sáu trở đi chỉ qua được
 * nếu hệ thống chờ đúng khoảng thời gian Gemini nói. Backoff tự nghĩ (1,2s rồi 2,4s) không bao giờ
 * đủ cho cửa sổ tính theo phút.
 * <p>
 * Cũng là phần dễ vỡ nhất khi Gemini đổi câu chữ thông báo lỗi — nên phải có test.
 */
class GeminiRetryHintTest {

    /** Không cần key thật: chỉ gọi hàm đọc chuỗi, không gọi mạng. */
    private final GeminiProvider provider = new GeminiProvider(
            org.springframework.web.reactive.function.client.WebClient.builder(),
            "", "gemini-3.6-flash", "gemini-embedding-001", 768);

    @Test
    @DisplayName("Đọc được số giây trong câu tiếng Anh Gemini trả về thật")
    void shouldParseRetryFromRealMessage() {
        // Nguyên văn thân lỗi bắt được lúc chạy kiểm chứng
        String body = """
                {"error":{"code":429,"message":"You exceeded your current quota, please check your plan
                and billing details. * Quota exceeded for metric:
                generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 5,
                model: gemini-3.6-flash\\nPlease retry in 52.031671855s.","status":"RESOURCE_EXHAUSTED"}}
                """;

        assertThat(provider.retryAfterMillis(body)).isEqualTo(52_032);
    }

    @Test
    @DisplayName("Đọc được trường retryDelay có cấu trúc")
    void shouldParseStructuredRetryDelay() {
        String body = """
                {"error":{"details":[{"@type":"type.googleapis.com/google.rpc.RetryInfo",
                "retryDelay":"30s"}]}}
                """;

        assertThat(provider.retryAfterMillis(body)).isEqualTo(30_000);
    }

    @ParameterizedTest
    @DisplayName("Không có gợi ý thì trả 0 để bên gọi dùng backoff mặc định")
    @ValueSource(strings = {
            "",
            "   ",
            "{\"error\":{\"code\":500,\"message\":\"Internal error\"}}",
            "Service temporarily unavailable"
    })
    void shouldReturnZeroWhenNoHint(String body) {
        assertThat(provider.retryAfterMillis(body)).isZero();
    }

    @Test
    @DisplayName("Thân lỗi null cũng không làm vỡ")
    void shouldHandleNullBody() {
        assertThat(provider.retryAfterMillis(null)).isZero();
    }
}
